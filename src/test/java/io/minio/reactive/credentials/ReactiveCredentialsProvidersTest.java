package io.minio.reactive.credentials;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.ProviderException;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

class ReactiveCredentialsProvidersTest {
  @Test
  void shouldBridgeStaticProviderToReactiveWithoutLeakingSecrets() {
    Credentials credentials =
        new Credentials("access-123456", "secret-abcdef", "session-token", ZonedDateTime.now().plusMinutes(5));

    ReactiveCredentials reactive =
        ReactiveCredentialsProvider.from(new StaticProvider(credentials)).getCredentials().block();

    Assertions.assertEquals("access-123456", reactive.accessKey());
    Assertions.assertEquals("secret-abcdef", reactive.secretKey());
    Assertions.assertEquals("session-token", reactive.sessionToken());
    Assertions.assertFalse(credentials.isExpired());
    Assertions.assertFalse(credentials.toString().contains("secret-abcdef"));
    Assertions.assertFalse(credentials.toString().contains("session-token"));
  }

  @Test
  void shouldReadEnvironmentProvidersFromSystemProperties() {
    withSystemProperty(
        "MINIO_ACCESS_KEY",
        "minio-ak",
        () ->
            withSystemProperty(
                "MINIO_SECRET_KEY",
                "minio-sk",
                () -> {
                  Credentials credentials = new MinioEnvironmentProvider().fetch();
                  Assertions.assertEquals("minio-ak", credentials.accessKey());
                  Assertions.assertEquals("minio-sk", credentials.secretKey());
                }));

    withSystemProperty(
        "AWS_ACCESS_KEY_ID",
        "aws-ak",
        () ->
            withSystemProperty(
                "AWS_SECRET_ACCESS_KEY",
                "aws-sk",
                () ->
                    withSystemProperty(
                        "AWS_SESSION_TOKEN",
                        "aws-session",
                        () -> {
                          Credentials credentials = new AwsEnvironmentProvider().fetch();
                          Assertions.assertEquals("aws-ak", credentials.accessKey());
                          Assertions.assertEquals("aws-sk", credentials.secretKey());
                          Assertions.assertEquals("aws-session", credentials.sessionToken());
                        })));
  }

  @Test
  void shouldRejectEmptyAwsPrimaryEnvironmentValue() {
    Assertions.assertThrows(
        ProviderException.class,
        () ->
            withSystemProperty(
                "AWS_ACCESS_KEY_ID",
                "",
                () ->
                    withSystemProperty(
                        "AWS_ACCESS_KEY",
                        "fallback-ak",
                        () ->
                            withSystemProperty(
                                "AWS_SECRET_ACCESS_KEY",
                                "aws-sk",
                                () -> new AwsEnvironmentProvider().fetch()))));

    Assertions.assertThrows(
        ProviderException.class,
        () ->
            withSystemProperty(
                "AWS_ACCESS_KEY_ID",
                "aws-ak",
                () ->
                    withSystemProperty(
                        "AWS_SECRET_ACCESS_KEY",
                        "",
                        () ->
                            withSystemProperty(
                                "AWS_SECRET_KEY",
                                "fallback-sk",
                                () -> new AwsEnvironmentProvider().fetch()))));
  }

  @Test
  void shouldDeserializeJwtWithMinioJavaFieldNames() throws Exception {
    ObjectMapper mapper = new ObjectMapper();

    Jwt jwt = mapper.readValue("{\"access_token\":\"json-token\",\"expires_in\":60}", Jwt.class);

    Assertions.assertEquals("json-token", jwt.token());
    Assertions.assertEquals(60, jwt.expiry());
    Assertions.assertTrue(mapper.writeValueAsString(jwt).contains("access_token"));
  }

  @Test
  void shouldChainProvidersAndSkipProviderException() {
    Provider failing =
        () -> {
          throw new ProviderException("模拟 provider 失败");
        };
    ChainedProvider provider = new ChainedProvider(failing, new StaticProvider("chain-ak", "chain-sk"));

    Credentials credentials = provider.fetch();

    Assertions.assertEquals("chain-ak", credentials.accessKey());
    Assertions.assertEquals("chain-sk", credentials.secretKey());
  }

  @Test
  void shouldReadAwsAndMinioConfigFilesWithoutPrintingSecrets() throws Exception {
    Path dir = Files.createTempDirectory("reactive-credentials-provider-");
    Path awsFile = dir.resolve("credentials");
    Files.write(
        awsFile,
        ("[dev]\n"
                + "aws_access_key_id = aws-file-ak\n"
                + "aws_secret_access_key = aws-file-sk\n"
                + "aws_session_token = aws-file-session\n")
            .getBytes(StandardCharsets.UTF_8));

    Credentials aws = new AwsConfigProvider(awsFile.toString(), "dev").fetch();

    Assertions.assertEquals("aws-file-ak", aws.accessKey());
    Assertions.assertEquals("aws-file-sk", aws.secretKey());
    Assertions.assertEquals("aws-file-session", aws.sessionToken());

    Path minioFile = dir.resolve("config.json");
    Files.write(
        minioFile,
        ("{\"hosts\":{\"local\":{\"accessKey\":\"mc-ak\",\"secretKey\":\"mc-sk\"}}}")
            .getBytes(StandardCharsets.UTF_8));

    Credentials minio = new MinioClientConfigProvider(minioFile.toString(), "local").fetch();

    Assertions.assertEquals("mc-ak", minio.accessKey());
    Assertions.assertEquals("mc-sk", minio.secretKey());
    Assertions.assertFalse(minio.toString().contains("mc-sk"));
  }

  @Test
  void shouldExposeIdentityProviderBridgesAndSafeIamBoundary() {
    Credentials credentials = new Credentials("sts-ak", "sts-sk", "sts-session", null);

    Assertions.assertEquals("sts-ak", new AssumeRoleProvider(Mono.just(credentials)).fetch().accessKey());
    Assertions.assertEquals(
        "sts-ak",
        WebIdentityProvider.fromReactive(Mono.just(ReactiveCredentials.of("sts-ak", "sts-sk", "sts-session")))
            .fetch()
            .accessKey());
    Assertions.assertEquals("jwt", new Jwt("jwt", 900).token());

    withSystemProperty(
        "AWS_WEB_IDENTITY_TOKEN_FILE",
        "/tmp/not-used-token",
        () ->
            Assertions.assertThrows(ProviderException.class, () -> new IamAwsProvider().fetch()));
  }


  @Test
  void shouldUseMinioJavaStyleProviderInAllClientBuilders() {
    Provider provider = new StaticProvider("builder-ak", "builder-sk");

    Assertions.assertNotNull(
        io.minio.reactive.ReactiveMinioClient.builder()
            .endpoint("http://localhost:9000")
            .region("us-east-1")
            .credentialsProvider(provider)
            .build());
    Assertions.assertNotNull(
        io.minio.reactive.ReactiveMinioAdminClient.builder()
            .endpoint("http://localhost:9000")
            .region("us-east-1")
            .credentialsProvider(provider)
            .build());
    Assertions.assertNotNull(
        io.minio.reactive.ReactiveMinioRawClient.builder()
            .endpoint("http://localhost:9000")
            .region("us-east-1")
            .credentialsProvider(provider)
            .build());
    Assertions.assertNotNull(
        io.minio.reactive.ReactiveMinioKmsClient.builder()
            .endpoint("http://localhost:9000")
            .region("us-east-1")
            .credentialsProvider(provider)
            .build());
    Assertions.assertNotNull(
        io.minio.reactive.ReactiveMinioStsClient.builder()
            .endpoint("http://localhost:9000")
            .region("us-east-1")
            .credentialsProvider(provider)
            .build());
    Assertions.assertNotNull(
        io.minio.reactive.ReactiveMinioMetricsClient.builder()
            .endpoint("http://localhost:9000")
            .region("us-east-1")
            .credentialsProvider(provider)
            .build());
    Assertions.assertNotNull(
        io.minio.reactive.ReactiveMinioHealthClient.builder()
            .endpoint("http://localhost:9000")
            .region("us-east-1")
            .credentialsProvider(provider)
            .build());
  }

  @Test
  void shouldFetchIdentityCredentialsThroughReactiveStsClient() {
    List<String> queries = new ArrayList<String>();
    WebClient webClient =
        WebClient.builder()
            .exchangeFunction(
                request -> {
                  queries.add(request.url().getQuery());
                  return Mono.just(ClientResponse.create(HttpStatus.OK).body(stsSuccessXml()).build());
                })
            .build();
    io.minio.reactive.ReactiveMinioStsClient stsClient =
        io.minio.reactive.ReactiveMinioStsClient.builder()
            .endpoint("http://localhost:9000")
            .region("us-east-1")
            .webClient(webClient)
            .credentials("ak", "sk")
            .build();

    Assertions.assertEquals("sts-access", AssumeRoleProvider.fromStsClient(stsClient).fetch().accessKey());
    Assertions.assertEquals(
        "sts-access",
        WebIdentityProvider.fromStsClient(stsClient, () -> new Jwt("web-token", 900)).fetch().accessKey());
    Assertions.assertEquals(
        "sts-access",
        ClientGrantsProvider.fromStsClient(stsClient, () -> new Jwt("client-token", 900)).fetch().accessKey());
    Assertions.assertEquals(
        "sts-access", LdapIdentityProvider.fromStsClient(stsClient, "ldap-user", "ldap-pass").fetch().accessKey());
    Assertions.assertEquals(
        "sts-access", CertificateIdentityProvider.fromStsClient(stsClient).fetch().accessKey());

    Assertions.assertTrue(containsQueryPart(queries, "WebIdentityToken=web-token"));
    Assertions.assertTrue(containsQueryPart(queries, "Token=client-token"));
    Assertions.assertTrue(containsQueryPart(queries, "LDAPUsername=ldap-user"));
    Assertions.assertTrue(containsQueryPart(queries, "LDAPPassword=ldap-pass"));
    Assertions.assertTrue(containsQueryPart(queries, "Action=AssumeRoleWithCertificate"));
  }

  private static void withSystemProperty(String key, String value, Runnable runnable) {
    String old = System.getProperty(key);
    try {
      System.setProperty(key, value);
      runnable.run();
    } finally {
      if (old == null) {
        System.clearProperty(key);
      } else {
        System.setProperty(key, old);
      }
    }
  }

  private static boolean containsQueryPart(List<String> queries, String part) {
    for (String query : queries) {
      if (query != null && query.contains(part)) {
        return true;
      }
    }
    return false;
  }

  private static String stsSuccessXml() {
    return "<AssumeRoleResponse><AssumeRoleResult><Credentials>"
        + "<AccessKeyId>sts-access</AccessKeyId>"
        + "<SecretAccessKey>sts-secret</SecretAccessKey>"
        + "<SessionToken>sts-token</SessionToken>"
        + "<Expiration>2030-01-01T00:00:00Z</Expiration>"
        + "</Credentials></AssumeRoleResult></AssumeRoleResponse>";
  }
}
