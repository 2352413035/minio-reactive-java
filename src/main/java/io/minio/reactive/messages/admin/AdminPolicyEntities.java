package io.minio.reactive.messages.admin;

import com.fasterxml.jackson.databind.JsonNode;
import io.minio.reactive.util.JsonSupport;

/** 内置策略绑定实体摘要，保留 MinIO 原始 JSON 以兼容字段漂移。 */
public final class AdminPolicyEntities extends AdminJsonResult {
  private final int userMappingCount;
  private final int groupMappingCount;
  private final int policyMappingCount;

  private AdminPolicyEntities(
      String rawJson, int userMappingCount, int groupMappingCount, int policyMappingCount) {
    super(rawJson, JsonSupport.parseMap(rawJson));
    this.userMappingCount = userMappingCount;
    this.groupMappingCount = groupMappingCount;
    this.policyMappingCount = policyMappingCount;
  }

  public static AdminPolicyEntities parse(String rawJson) {
    JsonNode root = JsonSupport.parseTree(rawJson);
    return new AdminPolicyEntities(
        rawJson,
        nodeSize(JsonSupport.child(root, "UserMappings", "userMappings", "users")),
        nodeSize(JsonSupport.child(root, "GroupMappings", "groupMappings", "groups")),
        nodeSize(JsonSupport.child(root, "PolicyMappings", "policyMappings", "policies")));
  }

  public int userMappingCount() {
    return userMappingCount;
  }

  public int groupMappingCount() {
    return groupMappingCount;
  }

  public int policyMappingCount() {
    return policyMappingCount;
  }

  public int totalMappingCount() {
    return userMappingCount + groupMappingCount + policyMappingCount;
  }

  private static int nodeSize(JsonNode node) {
    if (node == null || node.isNull()) {
      return 0;
    }
    if (node.isArray() || node.isObject()) {
      return node.size();
    }
    return 1;
  }
}
