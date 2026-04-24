package io.minio.reactive.messages;

/** 对象版本信息，来源于 ListObjectVersions 响应中的 Version 节点。 */
public final class ObjectVersionInfo {
  private final String key;
  private final String versionId;
  private final boolean latest;
  private final String lastModified;
  private final String etag;
  private final long size;
  private final String storageClass;
  private final String ownerId;
  private final String ownerDisplayName;

  public ObjectVersionInfo(
      String key,
      String versionId,
      boolean latest,
      String lastModified,
      String etag,
      long size,
      String storageClass,
      String ownerId,
      String ownerDisplayName) {
    this.key = key == null ? "" : key;
    this.versionId = versionId == null ? "" : versionId;
    this.latest = latest;
    this.lastModified = lastModified == null ? "" : lastModified;
    this.etag = etag == null ? "" : etag;
    this.size = size;
    this.storageClass = storageClass == null ? "" : storageClass;
    this.ownerId = ownerId == null ? "" : ownerId;
    this.ownerDisplayName = ownerDisplayName == null ? "" : ownerDisplayName;
  }

  public String key() { return key; }

  public String versionId() { return versionId; }

  public boolean latest() { return latest; }

  public String lastModified() { return lastModified; }

  public String etag() { return etag; }

  public long size() { return size; }

  public String storageClass() { return storageClass; }

  public String ownerId() { return ownerId; }

  public String ownerDisplayName() { return ownerDisplayName; }
}
