package io.minio.reactive.messages;

/** 对象删除标记信息，来源于 ListObjectVersions 响应中的 DeleteMarker 节点。 */
public final class DeleteMarkerInfo {
  private final String key;
  private final String versionId;
  private final boolean latest;
  private final String lastModified;
  private final String ownerId;
  private final String ownerDisplayName;

  public DeleteMarkerInfo(
      String key,
      String versionId,
      boolean latest,
      String lastModified,
      String ownerId,
      String ownerDisplayName) {
    this.key = key == null ? "" : key;
    this.versionId = versionId == null ? "" : versionId;
    this.latest = latest;
    this.lastModified = lastModified == null ? "" : lastModified;
    this.ownerId = ownerId == null ? "" : ownerId;
    this.ownerDisplayName = ownerDisplayName == null ? "" : ownerDisplayName;
  }

  public String key() { return key; }

  public String versionId() { return versionId; }

  public boolean latest() { return latest; }

  public String lastModified() { return lastModified; }

  public String ownerId() { return ownerId; }

  public String ownerDisplayName() { return ownerDisplayName; }
}
