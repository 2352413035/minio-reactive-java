package io.minio.reactive.messages.admin;

import com.fasterxml.jackson.databind.JsonNode;
import io.minio.reactive.util.JsonSupport;

/** 当前账号的管理端摘要，便于快速判断账号可访问 bucket 和策略信息。 */
public final class AdminAccountSummary extends AdminJsonResult {
  private final String accountName;
  private final int bucketCount;
  private final int readableBucketCount;
  private final int writableBucketCount;
  private final String policyJson;
  private final String backendType;

  private AdminAccountSummary(
      String rawJson,
      String accountName,
      int bucketCount,
      int readableBucketCount,
      int writableBucketCount,
      String policyJson,
      String backendType) {
    super(rawJson, JsonSupport.parseMap(rawJson));
    this.accountName = accountName;
    this.bucketCount = bucketCount;
    this.readableBucketCount = readableBucketCount;
    this.writableBucketCount = writableBucketCount;
    this.policyJson = policyJson;
    this.backendType = backendType;
  }

  public static AdminAccountSummary parse(String rawJson) {
    JsonNode root = JsonSupport.parseTree(rawJson);
    JsonNode buckets = JsonSupport.child(root, "Buckets", "buckets");
    JsonNode server = JsonSupport.child(root, "Server", "server");
    JsonNode backend = JsonSupport.child(server, "Backend", "backend");
    JsonNode policy = JsonSupport.child(root, "Policy", "policy");
    return new AdminAccountSummary(
        rawJson,
        JsonSupport.textAny(root, "AccountName", "accountName"),
        buckets == null || !buckets.isArray() ? 0 : buckets.size(),
        countBucketsByAccess(buckets, "read", "Read"),
        countBucketsByAccess(buckets, "write", "Write"),
        policy == null || policy.isNull() ? "" : policy.toString(),
        JsonSupport.textAny(backend == null ? server : backend, "Type", "type"));
  }

  private static int countBucketsByAccess(JsonNode buckets, String jsonName, String goName) {
    if (buckets == null || !buckets.isArray()) {
      return 0;
    }
    int count = 0;
    for (JsonNode bucket : buckets) {
      JsonNode access = JsonSupport.child(bucket, "access", "Access");
      if (JsonSupport.booleanAny(access, jsonName, goName)) {
        count++;
      }
    }
    return count;
  }

  public String accountName() {
    return accountName;
  }

  public int bucketCount() {
    return bucketCount;
  }

  public int readableBucketCount() {
    return readableBucketCount;
  }

  public int writableBucketCount() {
    return writableBucketCount;
  }

  public String policyJson() {
    return policyJson;
  }

  public String backendType() {
    return backendType;
  }
}
