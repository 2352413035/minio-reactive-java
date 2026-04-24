package io.minio.reactive.messages.admin;

import com.fasterxml.jackson.databind.JsonNode;
import io.minio.reactive.util.JsonSupport;

/** Admin health/OBD 诊断摘要，保留完整 JSON，同时提取集群状态、部署和对象规模字段。 */
public final class AdminHealthInfoSummary extends AdminJsonResult {
  private final String status;
  private final String version;
  private final String timestamp;
  private final String error;
  private final String deploymentId;
  private final String region;
  private final int serverCount;
  private final long bucketCount;
  private final long objectCount;

  private AdminHealthInfoSummary(
      String rawJson,
      String status,
      String version,
      String timestamp,
      String error,
      String deploymentId,
      String region,
      int serverCount,
      long bucketCount,
      long objectCount) {
    super(rawJson, JsonSupport.parseMap(rawJson));
    this.status = status;
    this.version = version;
    this.timestamp = timestamp;
    this.error = error;
    this.deploymentId = deploymentId;
    this.region = region;
    this.serverCount = serverCount;
    this.bucketCount = bucketCount;
    this.objectCount = objectCount;
  }

  public static AdminHealthInfoSummary parse(String rawJson) {
    JsonNode root = JsonSupport.parseTree(rawJson);
    String error = JsonSupport.textAny(root, "error", "Error");
    JsonNode minio = JsonSupport.child(root, "minio", "Minio");
    JsonNode info = JsonSupport.child(minio, "info", "Info");
    JsonNode buckets = JsonSupport.child(info, "buckets", "Buckets");
    JsonNode objects = JsonSupport.child(info, "objects", "Objects");
    return new AdminHealthInfoSummary(
        rawJson,
        error.isEmpty() ? "success" : "error",
        JsonSupport.textAny(root, "version", "Version"),
        JsonSupport.textAny(root, "timestamp", "TimeStamp", "timeStamp"),
        error,
        JsonSupport.textAny(info, "deploymentID", "DeploymentID", "deploymentId"),
        JsonSupport.textAny(info, "region", "Region"),
        JsonSupport.nodeSize(JsonSupport.child(info, "servers", "Servers")),
        JsonSupport.longAny(buckets, "count", "Count"),
        JsonSupport.longAny(objects, "count", "Count"));
  }

  public String status() {
    return status;
  }

  public String version() {
    return version;
  }

  public String timestamp() {
    return timestamp;
  }

  public String error() {
    return error;
  }

  public String deploymentId() {
    return deploymentId;
  }

  public String region() {
    return region;
  }

  public int serverCount() {
    return serverCount;
  }

  public long bucketCount() {
    return bucketCount;
  }

  public long objectCount() {
    return objectCount;
  }
}
