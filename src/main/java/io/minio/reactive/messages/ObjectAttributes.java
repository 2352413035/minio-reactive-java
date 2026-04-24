package io.minio.reactive.messages;

/**
 * GetObjectAttributes 响应摘要。
 *
 * <p>不同 MinIO/S3 版本返回字段可能不完全相同，因此模型提取高频稳定字段，同时保留原始 XML。
 */
public final class ObjectAttributes {
  private final String rawXml;
  private final String etag;
  private final long objectSize;
  private final String storageClass;
  private final String checksumCrc32;
  private final String checksumCrc32c;
  private final String checksumSha1;
  private final String checksumSha256;
  private final int totalPartsCount;

  public ObjectAttributes(
      String rawXml,
      String etag,
      long objectSize,
      String storageClass,
      String checksumCrc32,
      String checksumCrc32c,
      String checksumSha1,
      String checksumSha256,
      int totalPartsCount) {
    this.rawXml = rawXml == null ? "" : rawXml;
    this.etag = etag == null ? "" : etag;
    this.objectSize = objectSize;
    this.storageClass = storageClass == null ? "" : storageClass;
    this.checksumCrc32 = checksumCrc32 == null ? "" : checksumCrc32;
    this.checksumCrc32c = checksumCrc32c == null ? "" : checksumCrc32c;
    this.checksumSha1 = checksumSha1 == null ? "" : checksumSha1;
    this.checksumSha256 = checksumSha256 == null ? "" : checksumSha256;
    this.totalPartsCount = totalPartsCount;
  }

  public String rawXml() {
    return rawXml;
  }

  public String etag() {
    return etag;
  }

  public long objectSize() {
    return objectSize;
  }

  public String storageClass() {
    return storageClass;
  }

  public String checksumCrc32() {
    return checksumCrc32;
  }

  public String checksumCrc32c() {
    return checksumCrc32c;
  }

  public String checksumSha1() {
    return checksumSha1;
  }

  public String checksumSha256() {
    return checksumSha256;
  }

  public int totalPartsCount() {
    return totalPartsCount;
  }
}
