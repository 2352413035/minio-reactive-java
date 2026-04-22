package io.minio.reactive.messages;

/** 分片上传中单个 part 的元数据。 */
public final class PartInfo {
  private final int partNumber;
  private final String etag;
  private final long size;
  private final String lastModified;

  public PartInfo(int partNumber, String etag, long size, String lastModified) {
    this.partNumber = partNumber;
    this.etag = etag;
    this.size = size;
    this.lastModified = lastModified;
  }

  public int partNumber() {
    return partNumber;
  }

  public String etag() {
    return etag;
  }

  public long size() {
    return size;
  }

  public String lastModified() {
    return lastModified;
  }
}
