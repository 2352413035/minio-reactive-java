package io.minio.reactive.credentials;

/**
 * IAM/AWS 元数据 provider 迁移边界。
 *
 * <p>真实 IMDS/ECS/WebIdentity 网络换取凭证需要额外 SSRF 防护、超时和代理策略；当前阶段先显式
 * 暴露同名类和安全失败语义，避免悄悄访问 169.254.169.254 这类敏感地址。
 */
public final class IamAwsProvider extends EnvironmentProvider {
  private final String customEndpoint;

  public IamAwsProvider() {
    this(null);
  }

  public IamAwsProvider(String customEndpoint) {
    this.customEndpoint = customEndpoint;
  }

  @Override
  public Credentials fetch() {
    String tokenFile = getProperty("AWS_WEB_IDENTITY_TOKEN_FILE");
    if (tokenFile != null && !tokenFile.trim().isEmpty()) {
      throw new java.security.ProviderException(
          "IamAwsProvider 检测到 WebIdentity token 文件；请使用 ReactiveMinioStsClient 获取临时凭证后再接入对应 identity provider");
    }
    throw new java.security.ProviderException(
        "IamAwsProvider 暂未自动访问 IAM 元数据服务" + (customEndpoint == null ? "" : ": " + customEndpoint));
  }
}
