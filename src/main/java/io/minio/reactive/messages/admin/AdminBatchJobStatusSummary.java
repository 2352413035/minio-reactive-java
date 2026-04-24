package io.minio.reactive.messages.admin;

import com.fasterxml.jackson.databind.JsonNode;
import io.minio.reactive.util.JsonSupport;

/** Batch job 状态摘要，兼容 MinIO LastMetric 包装和旧版扁平状态 JSON。 */
public final class AdminBatchJobStatusSummary extends AdminJsonResult {
  private final String jobId;
  private final String jobType;
  private final String status;
  private final String startTime;
  private final String lastUpdate;
  private final int retryAttempts;
  private final boolean complete;
  private final boolean failed;

  private AdminBatchJobStatusSummary(
      String rawJson,
      String jobId,
      String jobType,
      String status,
      String startTime,
      String lastUpdate,
      int retryAttempts,
      boolean complete,
      boolean failed) {
    super(rawJson, JsonSupport.parseMap(rawJson));
    this.jobId = jobId;
    this.jobType = jobType;
    this.status = status;
    this.startTime = startTime;
    this.lastUpdate = lastUpdate;
    this.retryAttempts = retryAttempts;
    this.complete = complete;
    this.failed = failed;
  }

  public static AdminBatchJobStatusSummary parse(String rawJson) {
    JsonNode root = JsonSupport.parseTree(rawJson);
    JsonNode metric = JsonSupport.child(root, "LastMetric", "lastMetric", "metric", "Metric");
    JsonNode node = metric == null ? root : metric;
    boolean complete = JsonSupport.booleanAny(node, "Complete", "complete");
    boolean failed = JsonSupport.booleanAny(node, "Failed", "failed");
    return new AdminBatchJobStatusSummary(
        rawJson,
        JsonSupport.textAny(node, "JobID", "jobId", "id", "ID"),
        JsonSupport.textAny(node, "JobType", "jobType", "type", "Type"),
        statusText(root, node, complete, failed),
        JsonSupport.textAny(node, "StartTime", "startTime", "Started", "started"),
        JsonSupport.textAny(node, "LastUpdate", "lastUpdate"),
        JsonSupport.intAny(node, "RetryAttempts", "retryAttempts"),
        complete,
        failed);
  }

  private static String statusText(JsonNode root, JsonNode node, boolean complete, boolean failed) {
    String explicit = JsonSupport.textAny(node, "status", "Status");
    if (explicit.isEmpty() && node != root) {
      explicit = JsonSupport.textAny(root, "status", "Status");
    }
    if (!explicit.isEmpty()) {
      return explicit;
    }
    if (failed) {
      return "failed";
    }
    if (complete) {
      return "complete";
    }
    return "running";
  }

  public String jobId() {
    return jobId;
  }

  public String jobType() {
    return jobType;
  }

  public String status() {
    return status;
  }

  public String startTime() {
    return startTime;
  }

  public String lastUpdate() {
    return lastUpdate;
  }

  public int retryAttempts() {
    return retryAttempts;
  }

  public boolean complete() {
    return complete;
  }

  public boolean failed() {
    return failed;
  }
}
