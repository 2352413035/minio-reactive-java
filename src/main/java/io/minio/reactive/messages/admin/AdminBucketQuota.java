package io.minio.reactive.messages.admin;

import com.fasterxml.jackson.databind.JsonNode;
import io.minio.reactive.util.JsonSupport;

/** Bucket quota 查询结果，写入 quota 仍属于高风险操作，不在普通 live 测试中执行。 */
public final class AdminBucketQuota extends AdminJsonResult {
  private final long quota;
  private final long size;
  private final long rate;
  private final long requests;
  private final String quotaType;

  private AdminBucketQuota(
      String rawJson, long quota, long size, long rate, long requests, String quotaType) {
    super(rawJson, JsonSupport.parseMap(rawJson));
    this.quota = quota;
    this.size = size;
    this.rate = rate;
    this.requests = requests;
    this.quotaType = quotaType;
  }

  public static AdminBucketQuota parse(String rawJson) {
    JsonNode root = JsonSupport.parseTree(rawJson);
    return new AdminBucketQuota(
        rawJson,
        JsonSupport.longAny(root, "quota", "Quota"),
        JsonSupport.longAny(root, "size", "Size"),
        JsonSupport.longAny(root, "rate", "Rate"),
        JsonSupport.longAny(root, "requests", "Requests"),
        JsonSupport.textAny(root, "quotatype", "quotaType", "Type"));
  }

  public long quota() {
    return quota;
  }

  public long size() {
    return size;
  }

  public long rate() {
    return rate;
  }

  public long requests() {
    return requests;
  }

  public String quotaType() {
    return quotaType;
  }
}
