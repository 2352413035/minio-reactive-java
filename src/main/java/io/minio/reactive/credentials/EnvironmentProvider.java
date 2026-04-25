package io.minio.reactive.credentials;

/**
 * 环境变量/系统属性 provider 基类。
 *
 * <p>为了方便测试，读取顺序和 minio-java 一样：先系统属性，再环境变量。
 */
public abstract class EnvironmentProvider implements Provider {
  protected String getProperty(String name) {
    String value = System.getProperty(name);
    return value != null ? value : System.getenv(name);
  }

  protected String requireProperty(String name, String description) {
    String value = getProperty(name);
    if (value == null || value.trim().isEmpty()) {
      throw new java.security.ProviderException(description + " 不存在或为空: " + name);
    }
    return value;
  }
}
