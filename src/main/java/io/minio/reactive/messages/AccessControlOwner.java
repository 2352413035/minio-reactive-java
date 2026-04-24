package io.minio.reactive.messages;

/** ACL 所有者信息。 */
public final class AccessControlOwner {
  private final String id;
  private final String displayName;

  public AccessControlOwner(String id, String displayName) {
    this.id = id == null ? "" : id;
    this.displayName = displayName == null ? "" : displayName;
  }

  public String id() {
    return id;
  }

  public String displayName() {
    return displayName;
  }
}
