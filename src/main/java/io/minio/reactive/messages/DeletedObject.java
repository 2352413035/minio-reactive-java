package io.minio.reactive.messages;

/** Entry returned by DeleteObjects. */
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
