package io.minio.reactive.credentials;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.ProviderException;
import java.time.ZonedDateTime;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
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
}
