package io.minio.reactive.messages;

import io.minio.reactive.credentials.ReactiveCredentials;
import io.minio.reactive.messages.admin.AdminJsonResult;
import io.minio.reactive.messages.admin.AdminPolicyInfo;
import io.minio.reactive.messages.admin.AdminPolicyList;
import io.minio.reactive.messages.admin.AdminUserInfo;
import io.minio.reactive.messages.admin.AdminServerInfo;
import io.minio.reactive.messages.kms.KmsJsonResult;
import io.minio.reactive.messages.kms.KmsKeyStatus;
import io.minio.reactive.messages.kms.KmsKeyList;
import io.minio.reactive.messages.metrics.PrometheusMetricSample;
import io.minio.reactive.messages.metrics.PrometheusMetrics;
import io.minio.reactive.messages.sts.AssumeRoleResult;
import io.minio.reactive.messages.sts.AssumeRoleWithWebIdentityRequest;
import io.minio.reactive.util.S3Xml;
import io.minio.reactive.credentials.StsCredentialsProvider;
import reactor.core.publisher.Mono;
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
  void shouldParseAdminUserAndPolicyModels() {
    AdminUserInfo user =
        AdminUserInfo.parse("{\"policyName\":\"readonly\",\"status\":\"enabled\",\"memberOf\":[\"dev\"],\"updatedAt\":\"now\"}");
    AdminPolicyInfo policy =
        AdminPolicyInfo.parse("{\"PolicyName\":\"readonly\",\"Policy\":{\"Version\":\"2012-10-17\"},\"CreateDate\":\"c\",\"UpdateDate\":\"u\"}");
    AdminPolicyList list = AdminPolicyList.parse("{\"readonly\":{\"Version\":\"2012-10-17\"}}");

    Assertions.assertEquals("readonly", user.policyName());
    Assertions.assertEquals("enabled", user.status());
    Assertions.assertEquals("dev", user.memberOf().get(0));
    Assertions.assertEquals("readonly", policy.policyName());
    Assertions.assertTrue(policy.policyJson().contains("2012-10-17"));
    Assertions.assertTrue(list.policies().containsKey("readonly"));
  }

  @Test
  void shouldParseKmsKeyList() {
    KmsKeyList result = KmsKeyList.parse("{\"keys\":[\"k1\",\"k2\"]}");

    Assertions.assertEquals(2, result.keys().size());
    Assertions.assertEquals("k1", result.keys().get(0));
  }

  @Test
  void shouldParseKmsKeyStatus() {
    KmsKeyStatus status =
        KmsKeyStatus.parse("{\"key-id\":\"key1\",\"encryptionErr\":\"\",\"decryptionErr\":\"\"}");

    Assertions.assertEquals("key1", status.keyId());
    Assertions.assertTrue(status.isOk());
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
  void shouldCreateStsRequestAndProvider() {
    AssumeRoleWithWebIdentityRequest request = AssumeRoleWithWebIdentityRequest.of("token");
    AssumeRoleResult result =
        new AssumeRoleResult(ReactiveCredentials.of("ak", "sk", "session"), "later", "<xml/>");
    StsCredentialsProvider provider = StsCredentialsProvider.from(Mono.just(result));

    Assertions.assertEquals("token", request.webIdentityToken());
    Assertions.assertEquals("session", provider.getCredentials().block().sessionToken());
  }

  @Test
  void shouldParsePrometheusSamples() {
    PrometheusMetrics metrics =
        new PrometheusMetrics("cluster", "# HELP x demo\nminio_requests_total{method=\"GET\",code=\"200\"} 3\nplain_metric 4.5\n");

    java.util.List<PrometheusMetricSample> samples = metrics.samples();

    Assertions.assertEquals(2, samples.size());
    Assertions.assertEquals("minio_requests_total", samples.get(0).name());
    Assertions.assertEquals("GET", samples.get(0).labels().get("method"));
    Assertions.assertEquals(3.0, samples.get(0).value());
    Assertions.assertEquals("plain_metric", samples.get(1).name());
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
