package io.minio.reactive.messages.admin;

import com.fasterxml.jackson.databind.JsonNode;
import io.minio.reactive.util.JsonSupport;

/** batch job 启动响应摘要。 */
public final class AdminBatchJobStartResult extends AdminJsonResult {
  private final String jobId;
  private final String type;
  private final String user;
  private final String started;

  private AdminBatchJobStartResult(
      String rawJson, String jobId, String type, String user, String started) {
    super(rawJson, JsonSupport.parseMap(rawJson));
    this.jobId = jobId;
    this.type = type;
    this.user = user;
    this.started = started;
  }

  public static AdminBatchJobStartResult parse(String rawJson) {
    JsonNode root = JsonSupport.parseTree(rawJson);
    return new AdminBatchJobStartResult(
        rawJson,
        JsonSupport.textAny(root, "id", "ID", "jobId", "JobID"),
        JsonSupport.textAny(root, "type", "Type"),
        JsonSupport.textAny(root, "user", "User"),
        JsonSupport.textAny(root, "started", "Started"));
  }

  /** MinIO 返回的 batch job ID，后续 status/describe/cancel 都应使用它。 */
  public String jobId() {
    return jobId;
  }

  public String type() {
    return type;
  }

  public String user() {
    return user;
  }

  public String started() {
    return started;
  }
}
