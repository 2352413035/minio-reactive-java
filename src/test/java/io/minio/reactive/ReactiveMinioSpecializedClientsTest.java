package io.minio.reactive;

import io.minio.reactive.catalog.MinioApiCatalog;
import io.minio.reactive.messages.admin.AddServiceAccountRequest;
import io.minio.reactive.messages.admin.EncryptedAdminResponse;
import io.minio.reactive.messages.admin.AddUserRequest;
import io.minio.reactive.util.MadminEncryptionSupport;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

class ReactiveMinioSpecializedClientsTest {
  @Test
  void shouldExposePeerClientBuilders() {
    Assertions.assertNotNull(
        ReactiveMinioClient.builder().endpoint("http://localhost:9000").region("us-east-1").build());
    Assertions.assertNotNull(
        ReactiveMinioAdminClient.builder().endpoint("http://localhost:9000").region("us-east-1").build());
    Assertions.assertNotNull(
        ReactiveMinioKmsClient.builder().endpoint("http://localhost:9000").region("us-east-1").build());
    Assertions.assertNotNull(
        ReactiveMinioStsClient.builder().endpoint("http://localhost:9000").region("us-east-1").build());
    Assertions.assertNotNull(
        ReactiveMinioMetricsClient.builder().endpoint("http://localhost:9000").region("us-east-1").build());
    Assertions.assertNotNull(
        ReactiveMinioHealthClient.builder().endpoint("http://localhost:9000").region("us-east-1").build());
    Assertions.assertNotNull(
        ReactiveMinioRawClient.builder().endpoint("http://localhost:9000").region("us-east-1").build());
  }

  @Test
  void shouldExposeRepresentativeCatalogMethodsOnSpecializedClients() {
    assertMonoMethodExists(ReactiveMinioClient.class, "s3ListBuckets");
    assertMonoMethodExists(ReactiveMinioClient.class, "s3GetObject");
    assertMonoMethodExists(ReactiveMinioAdminClient.class, "serverInfo");
    assertMonoMethodExists(ReactiveMinioAdminClient.class, "addUser");
    assertMonoMethodExists(ReactiveMinioKmsClient.class, "keyStatus");
    assertMonoMethodExists(ReactiveMinioStsClient.class, "assumeRoleWithWebIdentity");
    assertMonoMethodExists(ReactiveMinioMetricsClient.class, "v3");
    assertMonoMethodExists(ReactiveMinioHealthClient.class, "liveGet");
  }

  @Test
  void shouldKeepSpecializedMethodsAlignedWithCatalogFamilies() {
    Assertions.assertTrue(countDistinctMonoMethods(ReactiveMinioClient.class, "s3") >= 77);
    Assertions.assertTrue(
        countDistinctMonoMethods(ReactiveMinioAdminClient.class, null)
            >= MinioApiCatalog.byFamily("admin").size());
    Assertions.assertTrue(
        countDistinctMonoMethods(ReactiveMinioKmsClient.class, null)
            >= MinioApiCatalog.byFamily("kms").size());
    Assertions.assertTrue(
        countDistinctMonoMethods(ReactiveMinioStsClient.class, null)
            >= MinioApiCatalog.byFamily("sts").size());
    Assertions.assertTrue(
        countDistinctMonoMethods(ReactiveMinioMetricsClient.class, null)
            >= MinioApiCatalog.byFamily("metrics").size());
    Assertions.assertTrue(
        countDistinctMonoMethods(ReactiveMinioHealthClient.class, null)
            >= MinioApiCatalog.byFamily("health").size());
  }


  @Test
  void shouldKeepAdvancedCompatibilityBaselineForMigration() {
    assertAdvancedBaseline(ReactiveMinioClient.class, 129, 15, 5);
    assertAdvancedBaseline(ReactiveMinioAdminClient.class, 201, 0, 0);
    assertAdvancedBaseline(ReactiveMinioKmsClient.class, 8, 0, 0);
    assertAdvancedBaseline(ReactiveMinioStsClient.class, 14, 0, 0);
    assertAdvancedBaseline(ReactiveMinioMetricsClient.class, 6, 0, 0);
    assertAdvancedBaseline(ReactiveMinioHealthClient.class, 0, 0, 0);
    assertAdvancedBaseline(ReactiveMinioRawClient.class, 3, 0, 8);
  }


  @Test
  void shouldNotExposeEndpointExecutorInPublicApi() {
    Class<?>[] publicTypes = {
      ReactiveMinioClient.class,
      ReactiveMinioAdminClient.class,
      ReactiveMinioKmsClient.class,
      ReactiveMinioStsClient.class,
      ReactiveMinioMetricsClient.class,
      ReactiveMinioHealthClient.class,
      ReactiveMinioRawClient.class
    };
    for (Class<?> type : publicTypes) {
      for (Method method : type.getMethods()) {
        Assertions.assertNotEquals(ReactiveMinioEndpointExecutor.class, method.getReturnType());
        for (Class<?> parameterType : method.getParameterTypes()) {
          Assertions.assertNotEquals(ReactiveMinioEndpointExecutor.class, parameterType);
        }
      }
    }
  }


  @Test
  void shouldTreatHealthBusinessMethodsAsBooleanResults() {
    WebClient webClient =
        WebClient.builder()
            .exchangeFunction(
                request ->
                    Mono.just(
                        ClientResponse.create(HttpStatus.SERVICE_UNAVAILABLE)
                            .body("not ready")
                            .build()))
            .build();
    ReactiveMinioHealthClient client =
        ReactiveMinioHealthClient.builder()
            .endpoint("http://localhost:9000")
            .region("us-east-1")
            .webClient(webClient)
            .build();

    Assertions.assertEquals(Boolean.FALSE, client.isReady().block());
    Assertions.assertEquals(503, client.checkReadiness().block().statusCode());
  }


  @Test
  void shouldBuildPolicyAndKmsBusinessRequests() {
    java.util.List<String> paths = new java.util.ArrayList<String>();
    java.util.List<String> queries = new java.util.ArrayList<String>();
    WebClient webClient =
        WebClient.builder()
            .exchangeFunction(
                request -> {
                  paths.add(request.url().getPath());
                  queries.add(request.url().getQuery());
                  return Mono.just(ClientResponse.create(HttpStatus.OK).body("{}").build());
                })
            .build();
    ReactiveMinioAdminClient admin =
        ReactiveMinioAdminClient.builder()
            .endpoint("http://localhost:9000")
            .region("us-east-1")
            .webClient(webClient)
            .credentials("ak", "sk")
            .build();
    ReactiveMinioKmsClient kms =
        ReactiveMinioKmsClient.builder()
            .endpoint("http://localhost:9000")
            .region("us-east-1")
            .webClient(webClient)
            .credentials("ak", "sk")
            .build();

    admin.putPolicy("readonly", "{\"Version\":\"2012-10-17\"}").block();
    admin.setUserPolicy("readonly", "user1").block();
    kms.getKeyStatus("key1").block();

    Assertions.assertTrue(paths.contains("/minio/admin/v3/add-canned-policy"));
    Assertions.assertTrue(queries.contains("name=readonly"));
    Assertions.assertTrue(containsAllQueryParts(queries, "policyName=readonly", "userOrGroup=user1", "isGroup=false"));
    Assertions.assertTrue(queries.contains("key-id=key1"));
  }



  @Test
  void shouldMarkMigratedS3CatalogMethodsDeprecated() {
    assertDeprecatedMethodExists(ReactiveMinioClient.class, "s3HeadObject");
    assertDeprecatedMethodExists(ReactiveMinioClient.class, "s3GetObject");
    assertDeprecatedMethodExists(ReactiveMinioClient.class, "s3PutObject");
    assertDeprecatedMethodExists(ReactiveMinioClient.class, "s3DeleteObject");
    assertDeprecatedMethodExists(ReactiveMinioClient.class, "s3ListObjectsV2");
    assertDeprecatedMethodExists(ReactiveMinioClient.class, "s3CreateMultipartUpload");
    assertDeprecatedMethodExists(ReactiveMinioClient.class, "s3PutObjectPart");
    assertDeprecatedMethodExists(ReactiveMinioClient.class, "s3CompleteMultipartUpload");
    assertDeprecatedMethodExists(ReactiveMinioClient.class, "s3AbortMultipartUpload");
  }



  @Test
  void shouldEncryptAddUserRequestPayload() {
    byte[] encrypted =
        MadminEncryptionSupport.encryptData(
            "admin-secret",
            io.minio.reactive.util.JsonSupport.toJsonBytes(
                AddUserRequest.of("user1", "user-secret").toPayload()));

    Assertions.assertTrue(MadminEncryptionSupport.isEncrypted(encrypted));
    Assertions.assertTrue(
        new String(
                MadminEncryptionSupport.decryptData("admin-secret", encrypted),
                java.nio.charset.StandardCharsets.UTF_8)
            .contains("user-secret"));
  }


  @Test
  void shouldBuildEncryptedServiceAccountRequestPayload() {
    AddServiceAccountRequest request =
        AddServiceAccountRequest.builder()
            .name("svc1")
            .description("demo service account")
            .policyJson("{\"Version\":\"2012-10-17\"}")
            .build();
    byte[] encrypted =
        MadminEncryptionSupport.encryptData(
            "admin-secret", io.minio.reactive.util.JsonSupport.toJsonBytes(request.toPayload()));
    String decrypted =
        new String(
            MadminEncryptionSupport.decryptData("admin-secret", encrypted),
            java.nio.charset.StandardCharsets.UTF_8);

    Assertions.assertTrue(MadminEncryptionSupport.isEncrypted(encrypted));
    Assertions.assertTrue(decrypted.contains("svc1"));
    Assertions.assertTrue(decrypted.contains("demo service account"));
  }


  @Test
  void shouldExposeServiceAccountBusinessMethod() {
    assertMonoMethodExists(ReactiveMinioAdminClient.class, "addServiceAccount");
    EncryptedAdminResponse response = new EncryptedAdminResponse(new byte[41]);
    Assertions.assertTrue(response.isEncrypted());
  }


  @Test
  void shouldExposeConfigWriteBusinessMethods() {
    assertMonoMethodExists(ReactiveMinioAdminClient.class, "setConfigKvText");
    assertMonoMethodExists(ReactiveMinioAdminClient.class, "setConfigText");
    ReactiveMinioAdminClient admin =
        ReactiveMinioAdminClient.builder()
            .endpoint("http://localhost:9000")
            .region("us-east-1")
            .credentials("ak", "sk")
            .build();

    Assertions.assertThrows(IllegalArgumentException.class, () -> admin.setConfigKvText(" "));
    Assertions.assertThrows(IllegalArgumentException.class, () -> admin.setConfigText(""));
  }


  private static void assertAdvancedBaseline(
      Class<?> type, int monoStringCount, int deprecatedCount, int rawishCount) {
    Assertions.assertEquals(monoStringCount, countPublicMonoStringMethods(type), type.getSimpleName());
    Assertions.assertEquals(deprecatedCount, countDeprecatedMethods(type), type.getSimpleName());
    Assertions.assertEquals(rawishCount, countRawishExecuteMethods(type), type.getSimpleName());
  }

  private static int countPublicMonoStringMethods(Class<?> type) {
    int count = 0;
    for (Method method : type.getDeclaredMethods()) {
      if (Modifier.isPublic(method.getModifiers())
          && method.getReturnType().equals(Mono.class)
          && method.getGenericReturnType().toString().contains("java.lang.String")) {
        count++;
      }
    }
    return count;
  }

  private static int countDeprecatedMethods(Class<?> type) {
    int count = 0;
    for (Method method : type.getDeclaredMethods()) {
      if (Modifier.isPublic(method.getModifiers()) && method.getAnnotation(Deprecated.class) != null) {
        count++;
      }
    }
    return count;
  }

  private static int countRawishExecuteMethods(Class<?> type) {
    int count = 0;
    for (Method method : type.getDeclaredMethods()) {
      if (Modifier.isPublic(method.getModifiers()) && method.getName().startsWith("executeTo")) {
        count++;
      }
    }
    return count;
  }

  private static void assertDeprecatedMethodExists(Class<?> type, String name) {
    for (Method method : type.getMethods()) {
      if (method.getName().equals(name) && method.getAnnotation(Deprecated.class) != null) {
        return;
      }
    }
    Assertions.fail("缺少 @Deprecated 方法: " + type.getSimpleName() + "." + name);
  }

  private static boolean containsAllQueryParts(java.util.List<String> queries, String... parts) {
    for (String query : queries) {
      boolean matched = true;
      for (String part : parts) {
        matched = matched && query != null && query.contains(part);
      }
      if (matched) {
        return true;
      }
    }
    return false;
  }

  private static void assertMonoMethodExists(Class<?> type, String name) {
    for (Method method : type.getMethods()) {
      if (method.getName().equals(name) && method.getReturnType().equals(Mono.class)) {
        return;
      }
    }
    Assertions.fail("缺少返回 Mono 的方法: " + type.getSimpleName() + "." + name);
  }

  private static int countDistinctMonoMethods(Class<?> type, String prefix) {
    Set<String> names = new HashSet<String>();
    for (Method method : type.getMethods()) {
      if (method.getDeclaringClass().equals(type)
          && method.getReturnType().equals(Mono.class)
          && (prefix == null || method.getName().startsWith(prefix))) {
        names.add(method.getName());
      }
    }
    return names.size();
  }
}
