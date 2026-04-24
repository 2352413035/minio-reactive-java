package io.minio.reactive.messages;

/** bucket 日志配置摘要。 */
public final class BucketLoggingConfiguration {
  private final String targetBucket;
  private final String targetPrefix;
  private final String rawXml;

  public BucketLoggingConfiguration(String targetBucket, String targetPrefix, String rawXml) {
    this.targetBucket = targetBucket == null ? "" : targetBucket;
    this.targetPrefix = targetPrefix == null ? "" : targetPrefix;
    this.rawXml = rawXml == null ? "" : rawXml;
  }

  public String targetBucket() {
    return targetBucket;
  }

  public String targetPrefix() {
    return targetPrefix;
  }

  public String rawXml() {
    return rawXml;
  }

  public boolean enabled() {
    return !targetBucket.isEmpty();
  }
}
