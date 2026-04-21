package io.minio.reactive.catalog;

import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class MinioApiCatalogTest {
  @Test
  void shouldExposeAllKnownMinioRouteFamilies() {
    Assertions.assertEquals(233, MinioApiCatalog.all().size());
    Assertions.assertEquals(77, MinioApiCatalog.byFamily("s3").size());
    Assertions.assertEquals(128, MinioApiCatalog.byFamily("admin").size());
    Assertions.assertEquals(7, MinioApiCatalog.byFamily("kms").size());
    Assertions.assertEquals(8, MinioApiCatalog.byFamily("health").size());
    Assertions.assertEquals(6, MinioApiCatalog.byFamily("metrics").size());
    Assertions.assertEquals(7, MinioApiCatalog.byFamily("sts").size());
  }

  @Test
  void shouldKeepEndpointNamesUnique() {
    Set<String> names = new HashSet<String>();
    for (MinioApiEndpoint endpoint : MinioApiCatalog.all()) {
      Assertions.assertTrue(names.add(endpoint.name()), "duplicate endpoint name: " + endpoint.name());
    }
  }

  @Test
  void shouldIncludeRepresentativeMinioInterfaces() {
    Assertions.assertEquals("/{bucket}/{object}", MinioApiCatalog.byName("S3_GET_OBJECT").pathTemplate());
    Assertions.assertEquals("/minio/admin/v3/info", MinioApiCatalog.byName("ADMIN_SERVER_INFO").pathTemplate());
    Assertions.assertEquals("/minio/kms/v1/key/status", MinioApiCatalog.byName("KMS_KEY_STATUS").pathTemplate());
    Assertions.assertFalse(MinioApiCatalog.byName("HEALTH_LIVE_GET").authRequired());
    Assertions.assertTrue(MinioApiCatalog.byName("METRICS_V3").authRequired());
    Assertions.assertEquals("bearer", MinioApiCatalog.byName("METRICS_V3").authScheme());
    Assertions.assertEquals("/minio/metrics/v3{pathComps}", MinioApiCatalog.byName("METRICS_V3").pathTemplate());
    Assertions.assertEquals("/", MinioApiCatalog.byName("STS_ASSUME_ROLE_WITH_WEB_IDENTITY").pathTemplate());
    Assertions.assertFalse(MinioApiCatalog.byName("STS_ASSUME_ROLE_WITH_WEB_IDENTITY").authRequired());
  }
}
