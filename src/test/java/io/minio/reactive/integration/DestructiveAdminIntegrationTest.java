package io.minio.reactive.integration;

import io.minio.reactive.ReactiveMinioAdminClient;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

/**
 * 破坏性 Admin 集成测试入口。
 *
 * <p>这类测试会修改 MinIO 服务端配置或远端目标，默认必须跳过；只有独立可回滚环境才能显式开启。
 */
class DestructiveAdminIntegrationTest {
  private static Map<String, String> cachedLabConfig;

  @Test
  void shouldRequireVerifiedLabEnvironmentBeforeDestructiveSuite() throws Exception {
    assumeDestructiveLabEnabled();
    assertVerifiedLabEnvironment();
  }

  @Test
  void shouldWriteAndRestoreConfigKvOnlyInsideVerifiedLab() throws Exception {
    assumeDestructiveLabEnabled();
    assertVerifiedLabEnvironment();
    String testConfigKv = labValue("MINIO_LAB_TEST_CONFIG_KV");
    String restoreConfigKv = labValue("MINIO_LAB_RESTORE_CONFIG_KV");
    Assumptions.assumeTrue(
        testConfigKv != null
            && !testConfigKv.trim().isEmpty()
            && restoreConfigKv != null
            && !restoreConfigKv.trim().isEmpty(),
        "缺少 MINIO_LAB_TEST_CONFIG_KV/MINIO_LAB_RESTORE_CONFIG_KV，跳过真实 config 写入与恢复。");

    ReactiveMinioAdminClient admin = labAdminClient();

    try {
      admin.setConfigKvText(testConfigKv).block();
      Assertions.assertNotNull(admin.getConfigHelp(configSubSystem(testConfigKv)).block());
    } finally {
      admin.setConfigKvText(restoreConfigKv).block();
    }
    Assertions.assertNotNull(admin.getConfigHelp(configSubSystem(restoreConfigKv)).block());
  }

  @Test
  void shouldWriteAndRestoreBucketQuotaOnlyInsideVerifiedLab() throws Exception {
    assumeDestructiveLabEnabled();
    assertVerifiedLabEnvironment();
    String bucket = labValue("MINIO_LAB_BUCKET");
    String testQuotaJson = labValue("MINIO_LAB_TEST_BUCKET_QUOTA_JSON");
    String restoreQuotaJson = labValue("MINIO_LAB_RESTORE_BUCKET_QUOTA_JSON");
    Assumptions.assumeTrue(
        notBlank(bucket) && notBlank(testQuotaJson) && notBlank(restoreQuotaJson),
        "缺少 bucket quota lab 配置，跳过真实 bucket quota 写入与恢复。");

    ReactiveMinioAdminClient admin = labAdminClient();
    try {
      admin.setBucketQuota(bucket, bytes(testQuotaJson), "application/json").block();
      Assertions.assertNotNull(admin.getBucketQuotaInfo(bucket).block());
    } finally {
      admin.setBucketQuota(bucket, bytes(restoreQuotaJson), "application/json").block();
    }
    Assertions.assertNotNull(admin.getBucketQuotaInfo(bucket).block());
  }

  @Test
  void shouldProbeTierRemoteTargetAndBatchFixturesOnlyInsideVerifiedLab() throws Exception {
    assumeDestructiveLabEnabled();
    assertVerifiedLabEnvironment();
    String tierName = labValue("MINIO_LAB_TIER_NAME");
    String bucket = labValue("MINIO_LAB_BUCKET");
    String remoteTargetType = getenvOrDefault("MINIO_LAB_REMOTE_TARGET_TYPE", "replication");
    boolean batchProbes = "true".equalsIgnoreCase(labValue("MINIO_LAB_ENABLE_BATCH_JOB_PROBES"));
    Assumptions.assumeTrue(
        notBlank(tierName) || notBlank(bucket) || batchProbes,
        "缺少 tier/remote target/batch job lab fixture，跳过可选 lab 探测。");

    ReactiveMinioAdminClient admin = labAdminClient();
    if (notBlank(tierName)) {
      Assertions.assertNotNull(admin.verifyTier(tierName).block());
    }
    if (notBlank(bucket)) {
      Assertions.assertNotNull(admin.listRemoteTargets(bucket, remoteTargetType).block());
    }
    if (batchProbes) {
      Assertions.assertNotNull(admin.listBatchJobs().block());
      Assertions.assertNotNull(admin.batchJobStatus().block());
      Assertions.assertNotNull(admin.describeBatchJob().block());
    }
  }

  private static void assumeDestructiveLabEnabled() {
    Assumptions.assumeTrue(
        "true".equalsIgnoreCase(labValue("MINIO_ALLOW_DESTRUCTIVE_ADMIN_TESTS")),
        "破坏性 Admin 测试默认跳过；需要独立可回滚 MinIO 环境并设置 MINIO_ALLOW_DESTRUCTIVE_ADMIN_TESTS=true");
  }

  private static void assertVerifiedLabEnvironment() throws Exception {
    ProcessBuilder builder =
        new ProcessBuilder("bash", "scripts/minio-lab/verify-env.sh")
            .directory(new java.io.File(System.getProperty("user.dir")))
            .redirectErrorStream(true);
    String configPath = labConfigPath();
    if (notBlank(configPath)) {
      builder.environment().put("MINIO_LAB_CONFIG_FILE", configPath);
    }
    Process process = builder.start();
    StringBuilder output = new StringBuilder();
    try (BufferedReader reader =
        new BufferedReader(new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
      String line;
      while ((line = reader.readLine()) != null) {
        output.append(line).append('\n');
      }
    }
    int exitCode = process.waitFor();
    Assertions.assertEquals(0, exitCode, output.toString());
  }

  private static ReactiveMinioAdminClient labAdminClient() {
    return ReactiveMinioAdminClient.builder()
        .endpoint(labValue("MINIO_LAB_ENDPOINT"))
        .region(getenvOrDefault("MINIO_LAB_REGION", "us-east-1"))
        .credentials(labValue("MINIO_LAB_ACCESS_KEY"), labValue("MINIO_LAB_SECRET_KEY"))
        .build();
  }

  private static String configSubSystem(String kvText) {
    String trimmed = kvText == null ? "" : kvText.trim();
    int space = trimmed.indexOf(' ');
    int equals = trimmed.indexOf('=');
    int end = space < 0 ? equals : equals < 0 ? space : Math.min(space, equals);
    return end <= 0 ? trimmed : trimmed.substring(0, end);
  }

  private static String getenvOrDefault(String name, String defaultValue) {
    String value = labValue(name);
    return value == null || value.trim().isEmpty() ? defaultValue : value;
  }

  private static byte[] bytes(String value) {
    return value.getBytes(StandardCharsets.UTF_8);
  }

  private static boolean notBlank(String value) {
    return value != null && !value.trim().isEmpty();
  }

  private static String labValue(String name) {
    String value = System.getenv(name);
    if (notBlank(value)) {
      return value;
    }
    return labConfig().get(name);
  }

  private static Map<String, String> labConfig() {
    if (cachedLabConfig != null) {
      return cachedLabConfig;
    }
    String configPath = labConfigPath();
    if (!notBlank(configPath)) {
      cachedLabConfig = Collections.emptyMap();
      return cachedLabConfig;
    }
    Path path = Paths.get(configPath);
    if (!Files.isRegularFile(path)) {
      cachedLabConfig = Collections.emptyMap();
      return cachedLabConfig;
    }
    Map<String, String> values = new LinkedHashMap<String, String>();
    try {
      for (String line : Files.readAllLines(path, StandardCharsets.UTF_8)) {
        String trimmed = line.trim();
        if (trimmed.isEmpty() || trimmed.startsWith("#") || !trimmed.contains("=")) {
          continue;
        }
        String key = trimmed.substring(0, trimmed.indexOf('=')).trim();
        String value = trimmed.substring(trimmed.indexOf('=') + 1);
        if (!key.matches("[A-Z0-9_]+")) {
          continue;
        }
        values.put(key, stripOuterQuotes(value));
      }
    } catch (Exception e) {
      throw new IllegalStateException("无法读取 destructive lab 配置文件: " + configPath, e);
    }
    cachedLabConfig = values;
    return cachedLabConfig;
  }

  private static String labConfigPath() {
    String value = System.getenv("MINIO_LAB_CONFIG_FILE");
    if (notBlank(value)) {
      return value;
    }
    String property = System.getProperty("minio.lab.config");
    if (notBlank(property)) {
      return property;
    }
    Path defaultPath = Paths.get(System.getProperty("user.dir"), "scripts", "minio-lab", "lab.properties");
    return Files.isRegularFile(defaultPath) ? defaultPath.toString() : "";
  }

  private static String stripOuterQuotes(String value) {
    if (value == null) {
      return "";
    }
    String trimmed = value.trim();
    if (trimmed.length() >= 2
        && ((trimmed.startsWith("\"") && trimmed.endsWith("\""))
            || (trimmed.startsWith("'") && trimmed.endsWith("'")))) {
      return trimmed.substring(1, trimmed.length() - 1);
    }
    return trimmed;
  }
}
