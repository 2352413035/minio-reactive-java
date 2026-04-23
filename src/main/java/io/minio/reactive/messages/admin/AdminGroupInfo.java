package io.minio.reactive.messages.admin;

import com.fasterxml.jackson.databind.JsonNode;
import io.minio.reactive.util.JsonSupport;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** 管理端用户组信息响应。 */
public final class AdminGroupInfo extends AdminJsonResult {
  private final String name;
  private final String status;
  private final List<String> members;
  private final String policy;
  private final String updatedAt;

  private AdminGroupInfo(String rawJson, String name, String status, List<String> members, String policy, String updatedAt) {
    super(rawJson, JsonSupport.parseMap(rawJson));
    this.name = name;
    this.status = status;
    this.members = Collections.unmodifiableList(new ArrayList<String>(members));
    this.policy = policy;
    this.updatedAt = updatedAt;
  }

  public static AdminGroupInfo parse(String rawJson) {
    JsonNode root = JsonSupport.parseTree(rawJson);
    List<String> members = new ArrayList<String>();
    JsonNode node = root.get("members");
    if (node != null && node.isArray()) {
      for (JsonNode member : node) {
        members.add(member.asText());
      }
    }
    return new AdminGroupInfo(
        rawJson,
        JsonSupport.text(root, "name"),
        JsonSupport.text(root, "status"),
        members,
        JsonSupport.text(root, "policy"),
        JsonSupport.text(root, "updatedAt"));
  }

  public String name() { return name; }
  public String status() { return status; }
  public List<String> members() { return members; }
  public String policy() { return policy; }
  public String updatedAt() { return updatedAt; }
}
