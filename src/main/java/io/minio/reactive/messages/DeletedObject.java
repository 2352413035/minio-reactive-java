package io.minio.reactive.messages;

/** 批量删除对象响应中的单个已删除对象记录。 */
public final class DeletedObject {
  private final String key;
  private final String versionId;
  private final boolean deleteMarker;
  private final String deleteMarkerVersionId;

  public DeletedObject(String key, String versionId, boolean deleteMarker, String deleteMarkerVersionId) {
    this.key = key;
    this.versionId = versionId;
    this.deleteMarker = deleteMarker;
    this.deleteMarkerVersionId = deleteMarkerVersionId;
  }

  public String key() {
    return key;
  }

  public String versionId() {
    return versionId;
  }

  public boolean deleteMarker() {
    return deleteMarker;
  }

  public String deleteMarkerVersionId() {
    return deleteMarkerVersionId;
  }
}
