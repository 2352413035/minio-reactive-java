package io.minio.reactive.messages;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** bucket 或对象 ACL 响应模型。 */
public final class AccessControlPolicy {
  private final AccessControlOwner owner;
  private final List<AccessControlGrant> grants;
  private final String rawXml;

  public AccessControlPolicy(
      AccessControlOwner owner, List<AccessControlGrant> grants, String rawXml) {
    this.owner = owner == null ? new AccessControlOwner("", "") : owner;
    this.grants =
        grants == null
            ? Collections.<AccessControlGrant>emptyList()
            : Collections.unmodifiableList(new ArrayList<AccessControlGrant>(grants));
    this.rawXml = rawXml == null ? "" : rawXml;
  }

  public AccessControlOwner owner() {
    return owner;
  }

  public List<AccessControlGrant> grants() {
    return grants;
  }

  public String rawXml() {
    return rawXml;
  }

  public boolean hasGrant(String permission) {
    if (permission == null) {
      return false;
    }
    for (AccessControlGrant grant : grants) {
      if (permission.equalsIgnoreCase(grant.permission())) {
        return true;
      }
    }
    return false;
  }
}
