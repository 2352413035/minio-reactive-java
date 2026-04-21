package io.minio.reactive.messages;

/** Multipart upload id returned by CreateMultipartUpload. */
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
