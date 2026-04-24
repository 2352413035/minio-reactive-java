package io.minio.reactive.messages;

/** bucket accelerate 配置摘要。 */
public final class BucketAccelerateConfiguration {
  private final String status;
  private final String rawXml;

  public BucketAccelerateConfiguration(String status, String rawXml) {
    this.status = status == null ? "" : status;
    this.rawXml = rawXml == null ? "" : rawXml;
  }

  public String status() {
    return status;
  }

  public String rawXml() {
    return rawXml;
  }

  public boolean enabled() {
    return "Enabled".equalsIgnoreCase(status);
  }
}
