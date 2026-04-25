package io.minio.reactive.credentials;

/** 使用 AWS 环境变量或系统属性的 provider。 */
public final class AwsEnvironmentProvider extends EnvironmentProvider {
  @Override
  public Credentials fetch() {
    String accessKey = firstNonBlank("AWS_ACCESS_KEY_ID", "AWS_ACCESS_KEY");
    String secretKey = firstNonBlank("AWS_SECRET_ACCESS_KEY", "AWS_SECRET_KEY");
    if (accessKey == null) {
      throw new java.security.ProviderException("AWS accessKey 不存在: AWS_ACCESS_KEY_ID/AWS_ACCESS_KEY");
    }
    if (secretKey == null) {
      throw new java.security.ProviderException("AWS secretKey 不存在: AWS_SECRET_ACCESS_KEY/AWS_SECRET_KEY");
    }
    return new Credentials(accessKey, secretKey, getProperty("AWS_SESSION_TOKEN"), null);
  }

  private String firstNonBlank(String first, String second) {
    String value = getProperty(first);
    if (value != null && !value.trim().isEmpty()) {
      return value;
    }
    value = getProperty(second);
    return value == null || value.trim().isEmpty() ? null : value;
  }
}
