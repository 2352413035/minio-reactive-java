package io.minio.reactive.credentials;

import java.security.ProviderException;

/** 使用 AWS 环境变量或系统属性的 provider。 */
public final class AwsEnvironmentProvider extends EnvironmentProvider {
  @Override
  public Credentials fetch() {
    return new Credentials(getAccessKey(), getSecretKey(), getValue("AWS_SESSION_TOKEN", "sessionToken"), null);
  }

  private String getAccessKey() {
    String value = getValue("AWS_ACCESS_KEY_ID", "AWS_ACCESS_KEY", "accessKey");
    if (value == null) {
      throw new ProviderException("AWS accessKey 不存在: AWS_ACCESS_KEY_ID/AWS_ACCESS_KEY");
    }
    return value;
  }

  private String getSecretKey() {
    String value = getValue("AWS_SECRET_ACCESS_KEY", "AWS_SECRET_KEY", "secretKey");
    if (value == null) {
      throw new ProviderException("AWS secretKey 不存在: AWS_SECRET_ACCESS_KEY/AWS_SECRET_KEY");
    }
    return value;
  }

  private String getValue(String primaryKey, String secondaryKey, String name) {
    String value = getValue(primaryKey, name);
    return value != null ? value : getValue(secondaryKey, name);
  }

  private String getValue(String key, String name) {
    String value = getProperty(key);
    if (value != null && value.isEmpty()) {
      throw new ProviderException(name + " 在 " + key + " 中为空");
    }
    return value;
  }
}
