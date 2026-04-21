package io.minio.reactive.messages;

/** Parsed CompleteMultipartUpload response. */
public final class CompletedMultipartUpload {
  private final String location;
  private final String bucket;
  private final String key;
  private final String etag;

  public CompletedMultipartUpload(String location, String bucket, String key, String etag) {
    this.location = location;
    this.bucket = bucket;
    this.key = key;
    this.etag = etag;
  }

  public String location() {
    return location;
  }

  public String bucket() {
    return bucket;
  }

  public String key() {
    return key;
  }

  public String etag() {
    return etag;
  }
}
