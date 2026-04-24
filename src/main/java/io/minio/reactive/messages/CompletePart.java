package io.minio.reactive.messages;

/** 完成分片上传时提交给服务端的分片描述。 */
public final class CompletePart {
  private final int partNumber;
  private final String etag;

  public CompletePart(int partNumber, String etag) {
    if (partNumber < 1) {
      throw new IllegalArgumentException("partNumber 必须为正数");
    }
    if (etag == null || etag.trim().isEmpty()) {
      throw new IllegalArgumentException("etag 不能为空");
    }
    this.partNumber = partNumber;
    this.etag = etag;
  }

  public int partNumber() {
    return partNumber;
  }

  public String etag() {
    return etag;
  }
}
