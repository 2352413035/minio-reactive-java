package io.minio.reactive.messages;

/** Bucket summary returned by S3 ListBuckets. */
public final class BucketInfo {
  private final String name;
  private final String creationDate;

  public BucketInfo(String name, String creationDate) {
    this.name = name;
    this.creationDate = creationDate;
  }

  public String name() {
    return name;
  }

  public String creationDate() {
    return creationDate;
  }

  @Override
  public String toString() {
    return "BucketInfo{name='" + name + "', creationDate='" + creationDate + "'}";
  }
}
