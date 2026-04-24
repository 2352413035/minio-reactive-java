package io.minio.reactive.messages.admin;

import com.fasterxml.jackson.databind.JsonNode;
import io.minio.reactive.util.JsonSupport;

/** 站点复制状态摘要，提取站点规模和各类差异明细数量，完整响应仍通过 rawJson 保留。 */
public final class AdminSiteReplicationStatusSummary extends AdminJsonResult {
  private final boolean enabled;
  private final int siteCount;
  private final int maxBuckets;
  private final int maxUsers;
  private final int maxGroups;
  private final int maxPolicies;
  private final int bucketStatsEntryCount;
  private final int policyStatsEntryCount;
  private final int userStatsEntryCount;
  private final int groupStatsEntryCount;
  private final int ilmExpiryStatsEntryCount;
  private final boolean metricsPresent;
  private final String apiVersion;

  private AdminSiteReplicationStatusSummary(
      String rawJson,
      boolean enabled,
      int siteCount,
      int maxBuckets,
      int maxUsers,
      int maxGroups,
      int maxPolicies,
      int bucketStatsEntryCount,
      int policyStatsEntryCount,
      int userStatsEntryCount,
      int groupStatsEntryCount,
      int ilmExpiryStatsEntryCount,
      boolean metricsPresent,
      String apiVersion) {
    super(rawJson, JsonSupport.parseMap(rawJson));
    this.enabled = enabled;
    this.siteCount = siteCount;
    this.maxBuckets = maxBuckets;
    this.maxUsers = maxUsers;
    this.maxGroups = maxGroups;
    this.maxPolicies = maxPolicies;
    this.bucketStatsEntryCount = bucketStatsEntryCount;
    this.policyStatsEntryCount = policyStatsEntryCount;
    this.userStatsEntryCount = userStatsEntryCount;
    this.groupStatsEntryCount = groupStatsEntryCount;
    this.ilmExpiryStatsEntryCount = ilmExpiryStatsEntryCount;
    this.metricsPresent = metricsPresent;
    this.apiVersion = apiVersion;
  }

  public static AdminSiteReplicationStatusSummary parse(String rawJson) {
    JsonNode root = JsonSupport.parseTree(rawJson);
    JsonNode metrics = JsonSupport.child(root, "Metrics", "metrics");
    return new AdminSiteReplicationStatusSummary(
        rawJson,
        JsonSupport.booleanAny(root, "Enabled", "enabled"),
        JsonSupport.nodeSize(JsonSupport.child(root, "Sites", "sites")),
        JsonSupport.intAny(root, "MaxBuckets", "maxBuckets"),
        JsonSupport.intAny(root, "MaxUsers", "maxUsers"),
        JsonSupport.intAny(root, "MaxGroups", "maxGroups"),
        JsonSupport.intAny(root, "MaxPolicies", "maxPolicies"),
        countNestedMapEntries(JsonSupport.child(root, "BucketStats", "bucketStats")),
        countNestedMapEntries(JsonSupport.child(root, "PolicyStats", "policyStats")),
        countNestedMapEntries(JsonSupport.child(root, "UserStats", "userStats")),
        countNestedMapEntries(JsonSupport.child(root, "GroupStats", "groupStats")),
        countNestedMapEntries(JsonSupport.child(root, "ILMExpiryStats", "ilmExpiryStats")),
        metrics != null && !metrics.isNull() && JsonSupport.nodeSize(metrics) > 0,
        JsonSupport.textAny(root, "apiVersion", "APIVersion"));
  }

  private static int countNestedMapEntries(JsonNode node) {
    if (node == null || !node.isObject()) {
      return 0;
    }
    int count = 0;
    java.util.Iterator<JsonNode> values = node.elements();
    while (values.hasNext()) {
      JsonNode value = values.next();
      count += JsonSupport.nodeSize(value);
    }
    return count;
  }

  public boolean enabled() {
    return enabled;
  }

  public int siteCount() {
    return siteCount;
  }

  public int maxBuckets() {
    return maxBuckets;
  }

  public int maxUsers() {
    return maxUsers;
  }

  public int maxGroups() {
    return maxGroups;
  }

  public int maxPolicies() {
    return maxPolicies;
  }

  public int bucketStatsEntryCount() {
    return bucketStatsEntryCount;
  }

  public int policyStatsEntryCount() {
    return policyStatsEntryCount;
  }

  public int userStatsEntryCount() {
    return userStatsEntryCount;
  }

  public int groupStatsEntryCount() {
    return groupStatsEntryCount;
  }

  public int ilmExpiryStatsEntryCount() {
    return ilmExpiryStatsEntryCount;
  }

  public boolean metricsPresent() {
    return metricsPresent;
  }

  public String apiVersion() {
    return apiVersion;
  }
}
