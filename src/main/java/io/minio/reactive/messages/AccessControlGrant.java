package io.minio.reactive.messages;

/** ACL 授权条目。 */
public final class AccessControlGrant {
  private final String granteeType;
  private final String id;
  private final String displayName;
  private final String uri;
  private final String emailAddress;
  private final String permission;

  public AccessControlGrant(
      String granteeType,
      String id,
      String displayName,
      String uri,
      String emailAddress,
      String permission) {
    this.granteeType = normalize(granteeType);
    this.id = normalize(id);
    this.displayName = normalize(displayName);
    this.uri = normalize(uri);
    this.emailAddress = normalize(emailAddress);
    this.permission = normalize(permission);
  }

  public String granteeType() {
    return granteeType;
  }

  public String id() {
    return id;
  }

  public String displayName() {
    return displayName;
  }

  public String uri() {
    return uri;
  }

  public String emailAddress() {
    return emailAddress;
  }

  public String permission() {
    return permission;
  }

  private static String normalize(String value) {
    return value == null ? "" : value.trim();
  }
}
