package io.minio.reactive.messages;

/** 分片上传会话信息，来源于 CreateMultipartUpload 响应。 */
public final class MultipartUpload {
  private final String bucket;
  private final String key;
  private final String uploadId;

  public MultipartUpload(String bucket, String key, String uploadId) {
    this.bucket = bucket;
    this.key = key;
    this.uploadId = uploadId;
  }

  public String bucket() {
    return bucket;
  }

  public String key() {
    return key;
  }

  public String uploadId() {
    return uploadId;
  }
}
