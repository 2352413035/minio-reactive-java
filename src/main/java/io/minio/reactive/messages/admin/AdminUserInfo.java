package io.minio.reactive.messages.admin;

import com.fasterxml.jackson.databind.JsonNode;
import io.minio.reactive.util.JsonSupport;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** 管理端用户信息响应。 */
public final class AdminUserInfo extends AdminJsonResult {
  private final String policyName;
  private final String status;
  private final List<String> memberOf;
  private final String updatedAt;

  private AdminUserInfo(
      String rawJson, String policyName, String status, List<String> memberOf, String updatedAt) {
    super(rawJson, JsonSupport.parseMap(rawJson));
    this.policyName = policyName;
    this.status = status;
    this.memberOf = Collections.unmodifiableList(new ArrayList<String>(memberOf));
    this.updatedAt = updatedAt;
  }

  public static AdminUserInfo parse(String rawJson) {
    JsonNode root = JsonSupport.parseTree(rawJson);
    List<String> groups = new ArrayList<String>();
    JsonNode memberOf = root.get("memberOf");
    if (memberOf != null && memberOf.isArray()) {
      for (JsonNode group : memberOf) {
        groups.add(group.asText());
      }
    }
    return new AdminUserInfo(
        rawJson,
        JsonSupport.text(root, "policyName"),
        JsonSupport.text(root, "status"),
        groups,
        JsonSupport.text(root, "updatedAt"));
  }

  public String policyName() {
    return policyName;
  }

  public String status() {
    return status;
  }

  public List<String> memberOf() {
    return memberOf;
  }

  public String updatedAt() {
    return updatedAt;
  }
}
