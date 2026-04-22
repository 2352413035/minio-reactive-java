package io.minio.reactive.messages;

/** 对象摘要以及常见响应元数据。 */
public final class ObjectInfo {
  private final String key;
  private final String lastModified;
  private final String etag;
  private final long size;
  private final String storageClass;

  public ObjectInfo(String key, String lastModified, String etag, long size, String storageClass) {
    this.key = key;
    this.lastModified = lastModified;
    this.etag = etag;
    this.size = size;
    this.storageClass = storageClass;
  }

  public String key() {
    return key;
  }

  public String lastModified() {
    return lastModified;
  }

  public String etag() {
    return etag;
  }

  public long size() {
    return size;
  }

  public String storageClass() {
    return storageClass;
  }

  @Override
  public String toString() {
    return "ObjectInfo{key='" + key + "', etag='" + etag + "', size=" + size + "}";
  }
}
