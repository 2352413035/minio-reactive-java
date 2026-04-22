package io.minio.reactive;

import io.minio.reactive.catalog.MinioApiCatalog;
import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
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
    Assertions.assertEquals(77, countDistinctMonoMethods(ReactiveMinioClient.class, "s3"));
    Assertions.assertEquals(
        MinioApiCatalog.byFamily("admin").size(),
        countDistinctMonoMethods(ReactiveMinioAdminClient.class, null));
    Assertions.assertEquals(
        MinioApiCatalog.byFamily("kms").size(),
        countDistinctMonoMethods(ReactiveMinioKmsClient.class, null));
    Assertions.assertEquals(
        MinioApiCatalog.byFamily("sts").size(),
        countDistinctMonoMethods(ReactiveMinioStsClient.class, null));
    Assertions.assertEquals(
        MinioApiCatalog.byFamily("metrics").size(),
        countDistinctMonoMethods(ReactiveMinioMetricsClient.class, null));
    Assertions.assertEquals(
        MinioApiCatalog.byFamily("health").size(),
        countDistinctMonoMethods(ReactiveMinioHealthClient.class, null));
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
