package io.minio.reactive.integration;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

/**
 * 破坏性 Admin 集成测试入口。
 *
 * <p>这类测试会修改 MinIO 服务端配置或远端目标，默认必须跳过；只有独立可回滚环境才能显式开启。
 */
class DestructiveAdminIntegrationTest {
  @Test
  void shouldStayDisabledUnlessExplicitlyAllowed() {
    Assumptions.assumeTrue(
        "true".equalsIgnoreCase(System.getenv("MINIO_ALLOW_DESTRUCTIVE_ADMIN_TESTS")),
        "破坏性 Admin 测试默认跳过；需要独立可回滚 MinIO 环境并设置 MINIO_ALLOW_DESTRUCTIVE_ADMIN_TESTS=true");
  }
}
