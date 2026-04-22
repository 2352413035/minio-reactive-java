package io.minio.reactive;

import io.minio.reactive.catalog.MinioApiCatalog;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.Map;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;

class ReactiveMinioSpecializedClientsTest {
  @Test
  void shouldExposeSpecializedClientFactories() {
    ReactiveMinioClient client =
        ReactiveMinioClient.builder().endpoint("http://localhost:9000").region("us-east-1").build();

    Assertions.assertNotNull(client.adminClient());
    Assertions.assertNotNull(client.kmsClient());
    Assertions.assertNotNull(client.stsClient());
    Assertions.assertNotNull(client.metricsClient());
    Assertions.assertNotNull(client.healthClient());
    Assertions.assertNotNull(client.rawClient());
  }

  @Test
  void shouldExposeRepresentativeCatalogMethodsOnSpecializedClients() throws Exception {
    assertMethod(ReactiveMinioClient.class, "s3ListBuckets");
    assertMethod(ReactiveMinioClient.class, "s3GetObject");
    assertMethod(ReactiveMinioAdminClient.class, "serverInfo");
    assertMethod(ReactiveMinioAdminClient.class, "addUser");
    assertMethod(ReactiveMinioKmsClient.class, "keyStatus");
    assertMethod(ReactiveMinioStsClient.class, "assumeRoleWithWebIdentity");
    assertMethod(ReactiveMinioMetricsClient.class, "v3");
    assertMethod(ReactiveMinioHealthClient.class, "liveGet");
  }

  @Test
  void shouldKeepSpecializedMethodsAlignedWithCatalogFamilies() {
    Assertions.assertEquals(77, countCatalogMethods(ReactiveMinioClient.class, "s3"));
    Assertions.assertEquals(MinioApiCatalog.byFamily("admin").size(), countCatalogMethods(ReactiveMinioAdminClient.class, null));
    Assertions.assertEquals(MinioApiCatalog.byFamily("kms").size(), countCatalogMethods(ReactiveMinioKmsClient.class, null));
    Assertions.assertEquals(MinioApiCatalog.byFamily("sts").size(), countCatalogMethods(ReactiveMinioStsClient.class, null));
    Assertions.assertEquals(MinioApiCatalog.byFamily("metrics").size(), countCatalogMethods(ReactiveMinioMetricsClient.class, null));
    Assertions.assertEquals(MinioApiCatalog.byFamily("health").size(), countCatalogMethods(ReactiveMinioHealthClient.class, null));
  }

  private static void assertMethod(Class<?> type, String name) throws Exception {
    Method method =
        type.getMethod(
            name,
            Map.class,
            Map.class,
            Map.class,
            byte[].class,
            String.class);
    Assertions.assertEquals(Mono.class, method.getReturnType());
  }

  private static int countCatalogMethods(Class<?> type, String prefix) {
    int count = 0;
    for (Method method : type.getMethods()) {
      if (method.getDeclaringClass().equals(type)
          && method.getParameterTypes().length == 5
          && method.getReturnType().equals(Mono.class)
          && (prefix == null || method.getName().startsWith(prefix))) {
        count++;
      }
    }
    return count;
  }
}
