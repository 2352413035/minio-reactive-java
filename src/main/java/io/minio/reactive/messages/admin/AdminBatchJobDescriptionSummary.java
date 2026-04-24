package io.minio.reactive.messages.admin;

import io.minio.reactive.util.JsonSupport;

/** Batch job 脱敏描述摘要；describe-job 返回 YAML，SDK 只提取安全元信息并保留原文。 */
public final class AdminBatchJobDescriptionSummary {
  private final String rawText;
  private final String id;
  private final String user;
  private final String started;
  private final String jobType;

  private AdminBatchJobDescriptionSummary(
      String rawText, String id, String user, String started, String jobType) {
    this.rawText = rawText == null ? "" : rawText;
    this.id = id == null ? "" : id;
    this.user = user == null ? "" : user;
    this.started = started == null ? "" : started;
    this.jobType = jobType == null ? "" : jobType;
  }

  public static AdminBatchJobDescriptionSummary parse(String rawText) {
    String text = rawText == null ? "" : rawText;
    if (text.trim().startsWith("{")) {
      com.fasterxml.jackson.databind.JsonNode root = JsonSupport.parseTree(text);
      return new AdminBatchJobDescriptionSummary(
          text,
          JsonSupport.textAny(root, "id", "ID"),
          JsonSupport.textAny(root, "user", "User"),
          JsonSupport.textAny(root, "started", "Started"),
          jsonJobType(root));
    }
    return new AdminBatchJobDescriptionSummary(
        text, yamlValue(text, "id"), yamlValue(text, "user"), yamlValue(text, "started"), yamlJobType(text));
  }

  private static String jsonJobType(com.fasterxml.jackson.databind.JsonNode root) {
    String explicit = JsonSupport.textAny(root, "type", "Type", "jobType", "JobType");
    if (!explicit.isEmpty()) {
      return explicit;
    }
    if (JsonSupport.child(root, "replicate", "Replicate") != null) {
      return "replicate";
    }
    if (JsonSupport.child(root, "keyrotate", "keyRotate", "KeyRotate") != null) {
      return "keyrotate";
    }
    if (JsonSupport.child(root, "expire", "Expire") != null) {
      return "expire";
    }
    return "";
  }

  private static String yamlValue(String text, String key) {
    String prefix = key + ":";
    String[] lines = text.split("\\r?\\n");
    for (String line : lines) {
      String trimmed = line.trim();
      if (trimmed.startsWith(prefix)) {
        return trimmed.substring(prefix.length()).trim();
      }
    }
    return "";
  }

  private static String yamlJobType(String text) {
    String[] lines = text.split("\\r?\\n");
    for (String line : lines) {
      String trimmed = line.trim();
      if ("replicate:".equals(trimmed)) {
        return "replicate";
      }
      if ("keyrotate:".equals(trimmed) || "keyRotate:".equals(trimmed)) {
        return "keyrotate";
      }
      if ("expire:".equals(trimmed)) {
        return "expire";
      }
    }
    return yamlValue(text, "type");
  }

  public String rawText() {
    return rawText;
  }

  public String id() {
    return id;
  }

  public String user() {
    return user;
  }

  public String started() {
    return started;
  }

  public String jobType() {
    return jobType;
  }
}
