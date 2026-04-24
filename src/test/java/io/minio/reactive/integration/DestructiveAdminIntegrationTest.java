package io.minio.reactive.integration;

import io.minio.reactive.ReactiveMinioAdminClient;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

/**
 * 破坏性 Admin 集成测试入口。
 *
 * <p>这类测试会修改 MinIO 服务端配置或远端目标，默认必须跳过；只有独立可回滚环境才能显式开启。
 */
class DestructiveAdminIntegrationTest {
  @Test
  void shouldRequireVerifiedLabEnvironmentBeforeDestructiveSuite() throws Exception {
    assumeDestructiveLabEnabled();
    assertVerifiedLabEnvironment();
  }

  @Test
  void shouldWriteAndRestoreConfigKvOnlyInsideVerifiedLab() throws Exception {
    assumeDestructiveLabEnabled();
    assertVerifiedLabEnvironment();
    String testConfigKv = System.getenv("MINIO_LAB_TEST_CONFIG_KV");
    String restoreConfigKv = System.getenv("MINIO_LAB_RESTORE_CONFIG_KV");
    Assumptions.assumeTrue(
        testConfigKv != null
            && !testConfigKv.trim().isEmpty()
            && restoreConfigKv != null
            && !restoreConfigKv.trim().isEmpty(),
        "缺少 MINIO_LAB_TEST_CONFIG_KV/MINIO_LAB_RESTORE_CONFIG_KV，跳过真实 config 写入与恢复。");

    ReactiveMinioAdminClient admin =
        ReactiveMinioAdminClient.builder()
            .endpoint(System.getenv("MINIO_LAB_ENDPOINT"))
            .region(getenvOrDefault("MINIO_LAB_REGION", "us-east-1"))
            .credentials(System.getenv("MINIO_LAB_ACCESS_KEY"), System.getenv("MINIO_LAB_SECRET_KEY"))
            .build();

    try {
      admin.setConfigKvText(testConfigKv).block();
      Assertions.assertNotNull(admin.getConfigHelp(configSubSystem(testConfigKv)).block());
    } finally {
      admin.setConfigKvText(restoreConfigKv).block();
    }
    Assertions.assertNotNull(admin.getConfigHelp(configSubSystem(restoreConfigKv)).block());
  }

  private static void assumeDestructiveLabEnabled() {
    Assumptions.assumeTrue(
        "true".equalsIgnoreCase(System.getenv("MINIO_ALLOW_DESTRUCTIVE_ADMIN_TESTS")),
        "破坏性 Admin 测试默认跳过；需要独立可回滚 MinIO 环境并设置 MINIO_ALLOW_DESTRUCTIVE_ADMIN_TESTS=true");
  }

  private static void assertVerifiedLabEnvironment() throws Exception {
    Process process =
        new ProcessBuilder("bash", "scripts/minio-lab/verify-env.sh")
            .directory(new java.io.File(System.getProperty("user.dir")))
            .redirectErrorStream(true)
            .start();
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

  private static String configSubSystem(String kvText) {
    String trimmed = kvText == null ? "" : kvText.trim();
    int space = trimmed.indexOf(' ');
    int equals = trimmed.indexOf('=');
    int end = space < 0 ? equals : equals < 0 ? space : Math.min(space, equals);
    return end <= 0 ? trimmed : trimmed.substring(0, end);
  }

  private static String getenvOrDefault(String name, String defaultValue) {
    String value = System.getenv(name);
    return value == null || value.trim().isEmpty() ? defaultValue : value;
  }
}
