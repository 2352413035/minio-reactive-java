package io.minio.reactive.messages;

import io.minio.reactive.credentials.ReactiveCredentials;
import io.minio.reactive.messages.admin.AdminJsonResult;
import io.minio.reactive.messages.admin.AdminServerInfo;
import io.minio.reactive.messages.kms.KmsJsonResult;
import io.minio.reactive.messages.kms.KmsKeyList;
import io.minio.reactive.messages.metrics.PrometheusMetrics;
import io.minio.reactive.messages.sts.AssumeRoleResult;
import io.minio.reactive.util.S3Xml;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class StrongBusinessModelsTest {
  @Test
  void shouldParseAdminServerInfoSummary() {
    AdminServerInfo info =
        AdminServerInfo.parse(
            "{\"mode\":\"online\",\"region\":\"us-east-1\",\"deploymentID\":\"dep\","
                + "\"buckets\":{\"count\":2},\"objects\":{\"count\":5},"
                + "\"servers\":[{\"state\":\"online\"},{\"state\":\"online\"}]}");

    Assertions.assertEquals("online", info.mode());
    Assertions.assertEquals("us-east-1", info.region());
    Assertions.assertEquals("dep", info.deploymentId());
    Assertions.assertEquals(2, info.bucketCount());
    Assertions.assertEquals(5L, info.objectCount());
    Assertions.assertEquals(2, info.serverCount());
    Assertions.assertTrue(info.rawJson().contains("deploymentID"));
  }

  @Test
  void shouldKeepUnknownAdminJsonFields() {
    AdminJsonResult result = AdminJsonResult.parse("{\"a\":1,\"b\":\"x\"}");

    Assertions.assertEquals(1, result.values().get("a"));
    Assertions.assertEquals("x", result.values().get("b"));
  }

  @Test
  void shouldParseKmsKeyList() {
    KmsKeyList result = KmsKeyList.parse("{\"keys\":[\"k1\",\"k2\"]}");

    Assertions.assertEquals(2, result.keys().size());
    Assertions.assertEquals("k1", result.keys().get(0));
  }

  @Test
  void shouldWrapKmsJson() {
    KmsJsonResult result = KmsJsonResult.parse("{\"status\":\"ok\"}");

    Assertions.assertEquals("ok", result.values().get("status"));
  }

  @Test
  void shouldParseStsCredentials() {
    AssumeRoleResult result =
        S3Xml.parseAssumeRoleResult(
            "<AssumeRoleWithWebIdentityResponse><AssumeRoleWithWebIdentityResult><Credentials>"
                + "<AccessKeyId>ak</AccessKeyId><SecretAccessKey>sk</SecretAccessKey>"
                + "<SessionToken>token</SessionToken><Expiration>tomorrow</Expiration>"
                + "</Credentials></AssumeRoleWithWebIdentityResult></AssumeRoleWithWebIdentityResponse>");

    ReactiveCredentials credentials = result.credentials();
    Assertions.assertEquals("ak", credentials.accessKey());
    Assertions.assertEquals("sk", credentials.secretKey());
    Assertions.assertEquals("token", credentials.sessionToken());
    Assertions.assertEquals("tomorrow", result.expiration());
  }

  @Test
  void shouldExposeHealthAndMetricsBusinessObjects() {
    HealthCheckResult health = new HealthCheckResult("live", 200);
    PrometheusMetrics metrics = new PrometheusMetrics("cluster", "# HELP x\nx 1\n");

    Assertions.assertTrue(health.isHealthy());
    Assertions.assertFalse(metrics.isEmpty());
    Assertions.assertEquals("cluster", metrics.scope());
  }
}
