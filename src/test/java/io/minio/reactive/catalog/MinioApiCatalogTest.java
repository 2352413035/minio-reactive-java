package io.minio.reactive.catalog;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
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
  void shouldMatchGeneratedMinioRouteBaseline() throws Exception {
    InputStream input = getClass().getResourceAsStream("/minio-route-baseline.json");
    Assertions.assertNotNull(input, "缺少由 scripts/report-route-parity.py 生成的路由基线");

    JsonNode routes = new ObjectMapper().readTree(input).get("routes");
    Map<String, Integer> expected = new LinkedHashMap<String, Integer>();
    for (JsonNode route : routes) {
      increment(expected, signature(route));
    }

    Map<String, Integer> actual = new LinkedHashMap<String, Integer>();
    for (MinioApiEndpoint endpoint : MinioApiCatalog.all()) {
      increment(actual, signature(endpoint));
    }

    Assertions.assertEquals(expected, actual, "SDK catalog 与 MinIO router 基线不一致");
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

  private static void increment(Map<String, Integer> counts, String key) {
    Integer current = counts.get(key);
    counts.put(key, current == null ? 1 : current + 1);
  }

  private static String signature(MinioApiEndpoint endpoint) {
    return endpoint.family()
        + " "
        + endpoint.method()
        + " "
        + endpoint.pathTemplate()
        + " auth="
        + endpoint.authScheme()
        + " defaultQuery="
        + queryText(endpoint.defaultQueryParameters())
        + " requiredQuery="
        + listText(endpoint.requiredQueryParameters());
  }

  private static String signature(JsonNode route) {
    return route.get("family").asText()
        + " "
        + route.get("method").asText()
        + " "
        + route.get("pathTemplate").asText()
        + " auth="
        + route.get("authScheme").asText()
        + " defaultQuery="
        + queryText(route.get("defaultQueryParameters"))
        + " requiredQuery="
        + listText(route.get("requiredQueryParameters"));
  }

  private static String queryText(Map<String, String> values) {
    Map<String, String> sorted = new TreeMap<String, String>(values);
    if (sorted.isEmpty()) {
      return "-";
    }
    List<String> parts = new ArrayList<String>();
    for (Map.Entry<String, String> entry : sorted.entrySet()) {
      parts.add(entry.getKey() + "=" + entry.getValue());
    }
    return parts.toString();
  }

  private static String queryText(JsonNode values) {
    Map<String, String> sorted = new TreeMap<String, String>();
    Iterator<String> names = values.fieldNames();
    while (names.hasNext()) {
      String name = names.next();
      sorted.put(name, values.get(name).asText());
    }
    return queryText(sorted);
  }

  private static String listText(List<String> values) {
    List<String> sorted = new ArrayList<String>(values);
    Collections.sort(sorted);
    return sorted.isEmpty() ? "-" : sorted.toString();
  }

  private static String listText(JsonNode values) {
    List<String> sorted = new ArrayList<String>();
    for (JsonNode value : values) {
      sorted.add(value.asText());
    }
    return listText(sorted);
  }
}
