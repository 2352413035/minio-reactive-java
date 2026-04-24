package io.minio.reactive.messages.admin;

import com.fasterxml.jackson.databind.JsonNode;
import io.minio.reactive.util.JsonSupport;

/** 站点复制元信息摘要，提取本地站点 metadata 规模，完整明细仍保留在 rawJson。 */
public final class AdminSiteReplicationMetaInfoSummary extends AdminJsonResult {
  private final boolean enabled;
  private final String name;
  private final String deploymentId;
  private final int bucketCount;
  private final int policyCount;
  private final int userCount;
  private final int groupCount;
  private final int replicationConfigCount;
  private final int ilmExpiryRuleCount;
  private final String apiVersion;

  private AdminSiteReplicationMetaInfoSummary(
      String rawJson,
      boolean enabled,
      String name,
      String deploymentId,
      int bucketCount,
      int policyCount,
      int userCount,
      int groupCount,
      int replicationConfigCount,
      int ilmExpiryRuleCount,
      String apiVersion) {
    super(rawJson, JsonSupport.parseMap(rawJson));
    this.enabled = enabled;
    this.name = name;
    this.deploymentId = deploymentId;
    this.bucketCount = bucketCount;
    this.policyCount = policyCount;
    this.userCount = userCount;
    this.groupCount = groupCount;
    this.replicationConfigCount = replicationConfigCount;
    this.ilmExpiryRuleCount = ilmExpiryRuleCount;
    this.apiVersion = apiVersion;
  }

  public static AdminSiteReplicationMetaInfoSummary parse(String rawJson) {
    JsonNode root = JsonSupport.parseTree(rawJson);
    return new AdminSiteReplicationMetaInfoSummary(
        rawJson,
        JsonSupport.booleanAny(root, "Enabled", "enabled"),
        JsonSupport.textAny(root, "Name", "name"),
        JsonSupport.textAny(root, "DeploymentID", "deploymentID", "deploymentId"),
        JsonSupport.nodeSize(JsonSupport.child(root, "Buckets", "buckets")),
        JsonSupport.nodeSize(JsonSupport.child(root, "Policies", "policies")),
        JsonSupport.nodeSize(JsonSupport.child(root, "UserInfoMap", "userInfoMap", "users")),
        JsonSupport.nodeSize(JsonSupport.child(root, "GroupDescMap", "groupDescMap", "groups")),
        JsonSupport.nodeSize(JsonSupport.child(root, "ReplicationCfg", "replicationCfg")),
        JsonSupport.nodeSize(JsonSupport.child(root, "ILMExpiryRules", "ilmExpiryRules")),
        JsonSupport.textAny(root, "apiVersion", "APIVersion"));
  }

  public boolean enabled() {
    return enabled;
  }

  public String name() {
    return name;
  }

  public String deploymentId() {
    return deploymentId;
  }

  public int bucketCount() {
    return bucketCount;
  }

  public int policyCount() {
    return policyCount;
  }

  public int userCount() {
    return userCount;
  }

  public int groupCount() {
    return groupCount;
  }

  public int replicationConfigCount() {
    return replicationConfigCount;
  }

  public int ilmExpiryRuleCount() {
    return ilmExpiryRuleCount;
  }

  public String apiVersion() {
    return apiVersion;
  }
}
