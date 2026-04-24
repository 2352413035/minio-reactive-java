package io.minio.reactive.messages.admin;

import com.fasterxml.jackson.databind.JsonNode;
import io.minio.reactive.util.JsonSupport;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Replication MRF backlog 摘要；兼容 MinIO 按行 JSON 流和测试环境里的单个 JSON 对象。 */
public final class AdminReplicationMrfSummary {
  private final String rawText;
  private final List<Entry> entries;

  private AdminReplicationMrfSummary(String rawText, List<Entry> entries) {
    this.rawText = rawText == null ? "" : rawText;
    this.entries = Collections.unmodifiableList(new ArrayList<Entry>(entries));
  }

  public static AdminReplicationMrfSummary parse(String rawText) {
    String text = rawText == null ? "" : rawText;
    List<Entry> result = new ArrayList<Entry>();
    String trimmed = text.trim();
    if (trimmed.startsWith("[")) {
      appendArray(result, JsonSupport.parseTree(trimmed));
    } else {
      appendLines(result, text);
    }
    return new AdminReplicationMrfSummary(text, result);
  }

  private static void appendLines(List<Entry> result, String text) {
    // MinIO handler 会周期性写入空格作为 keep-alive；这些空行/空格不代表 backlog 条目。
    String[] lines = text.split("\\r?\\n");
    for (String line : lines) {
      String trimmed = line.trim();
      if (trimmed.isEmpty()) {
        continue;
      }
      JsonNode node = JsonSupport.parseTree(trimmed);
      if (node.isArray()) {
        appendArray(result, node);
      } else if (node.isObject()) {
        result.add(Entry.from(node));
      }
    }
  }

  private static void appendArray(List<Entry> result, JsonNode array) {
    if (array == null || !array.isArray()) {
      return;
    }
    for (JsonNode item : array) {
      if (item != null && item.isObject()) {
        result.add(Entry.from(item));
      }
    }
  }

  public String rawText() {
    return rawText;
  }

  public List<Entry> entries() {
    return entries;
  }

  public int entryCount() {
    return entries.size();
  }

  public int errorCount() {
    int count = 0;
    for (Entry entry : entries) {
      if (!entry.error().isEmpty()) {
        count++;
      }
    }
    return count;
  }

  public int totalRetryCount() {
    int total = 0;
    for (Entry entry : entries) {
      total += entry.retryCount();
    }
    return total;
  }

  /** 单条 MRF backlog 记录摘要。 */
  public static final class Entry {
    private final String nodeName;
    private final String bucket;
    private final String object;
    private final String versionId;
    private final int retryCount;
    private final String error;

    private Entry(
        String nodeName,
        String bucket,
        String object,
        String versionId,
        int retryCount,
        String error) {
      this.nodeName = nodeName == null ? "" : nodeName;
      this.bucket = bucket == null ? "" : bucket;
      this.object = object == null ? "" : object;
      this.versionId = versionId == null ? "" : versionId;
      this.retryCount = retryCount;
      this.error = error == null ? "" : error;
    }

    private static Entry from(JsonNode node) {
      return new Entry(
          JsonSupport.textAny(node, "nodeName", "NodeName"),
          JsonSupport.textAny(node, "bucket", "Bucket"),
          JsonSupport.textAny(node, "object", "Object"),
          JsonSupport.textAny(node, "versionId", "VersionID", "versionID"),
          JsonSupport.intAny(node, "retryCount", "RetryCount"),
          JsonSupport.textAny(node, "error", "Err", "err"));
    }

    public String nodeName() {
      return nodeName;
    }

    public String bucket() {
      return bucket;
    }

    public String object() {
      return object;
    }

    public String versionId() {
      return versionId;
    }

    public int retryCount() {
      return retryCount;
    }

    public String error() {
      return error;
    }
  }
}
