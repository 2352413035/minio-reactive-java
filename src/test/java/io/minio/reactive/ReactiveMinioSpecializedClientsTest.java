package io.minio.reactive;

import io.minio.reactive.catalog.MinioApiCatalog;
import java.lang.reflect.Method;
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
