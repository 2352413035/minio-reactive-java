package io.minio.reactive.messages.admin;

import com.fasterxml.jackson.databind.JsonNode;
import io.minio.reactive.util.JsonSupport;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** 批处理任务列表摘要。 */
public final class AdminBatchJobList extends AdminJsonResult {
  private final List<Job> jobs;

  private AdminBatchJobList(String rawJson, List<Job> jobs) {
    super(rawJson, safeMap(rawJson));
    this.jobs = Collections.unmodifiableList(new ArrayList<Job>(jobs));
  }

  public static AdminBatchJobList parse(String rawJson) {
    JsonNode root = JsonSupport.parseTree(rawJson);
    JsonNode array = JsonSupport.child(root, "Jobs", "jobs", "Items", "items");
    if (array == null && root != null && root.isArray()) {
      array = root;
    }
    List<Job> result = new ArrayList<Job>();
    if (array != null && array.isArray()) {
      for (JsonNode item : array) {
        result.add(
            new Job(
                JsonSupport.textAny(item, "ID", "id", "JobID", "jobId"),
                JsonSupport.textAny(item, "Type", "type"),
                JsonSupport.textAny(item, "Status", "status"),
                JsonSupport.textAny(item, "User", "user")));
      }
    }
    return new AdminBatchJobList(rawJson, result);
  }

  public List<Job> jobs() {
    return jobs;
  }

  public int jobCount() {
    return jobs.size();
  }

  /** 单个 batch job 的公开摘要。 */
  public static final class Job {
    private final String id;
    private final String type;
    private final String status;
    private final String user;

    private Job(String id, String type, String status, String user) {
      this.id = id;
      this.type = type;
      this.status = status;
      this.user = user;
    }

    public String id() {
      return id;
    }

    public String type() {
      return type;
    }

    public String status() {
      return status;
    }

    public String user() {
      return user;
    }
  }

  private static java.util.Map<String, Object> safeMap(String rawJson) {
    JsonNode root = JsonSupport.parseTree(rawJson);
    return root != null && root.isObject()
        ? JsonSupport.parseMap(rawJson)
        : java.util.Collections.<String, Object>emptyMap();
  }
}
