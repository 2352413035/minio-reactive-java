package io.minio.reactive.messages;

/** bucket policy status 响应。 */
public final class BucketPolicyStatus {
  private final boolean publicBucket;
  private final String rawXml;

  public BucketPolicyStatus(boolean publicBucket, String rawXml) {
    this.publicBucket = publicBucket;
    this.rawXml = rawXml == null ? "" : rawXml;
  }

  public boolean publicBucket() {
    return publicBucket;
  }

  public String rawXml() {
    return rawXml;
  }
}
