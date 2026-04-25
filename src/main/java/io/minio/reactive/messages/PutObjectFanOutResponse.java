package io.minio.reactive.messages;

import io.minio.reactive.util.JsonSupport;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/** FanOut 上传响应，包含每个目标对象的独立结果。 */
public final class PutObjectFanOutResponse {
  private final String bucket;
  private final List<PutObjectFanOutResult> results;
  private final String rawText;

  public PutObjectFanOutResponse(String bucket, List<PutObjectFanOutResult> results, String rawText) {
    this.bucket = bucket;
    this.results = Collections.unmodifiableList(new ArrayList<PutObjectFanOutResult>(results));
    this.rawText = rawText == null ? "" : rawText;
  }

  public String bucket() {
    return bucket;
  }

  public List<PutObjectFanOutResult> results() {
    return results;
  }

  public String rawText() {
    return rawText;
  }

  public int resultCount() {
    return results.size();
  }

  public static PutObjectFanOutResponse parse(String bucket, String rawText) {
    List<PutObjectFanOutResult> values = new ArrayList<PutObjectFanOutResult>();
    for (String item : splitJsonObjects(rawText)) {
      Map<String, Object> map = JsonSupport.parseMap(item);
      values.add(
          new PutObjectFanOutResult(
              stringValue(map.get("key")),
              stringValue(map.get("etag")),
              stringValue(map.get("versionId")),
              stringValue(map.get("error"))));
    }
    return new PutObjectFanOutResponse(bucket, values, rawText);
  }

  private static List<String> splitJsonObjects(String rawText) {
    List<String> values = new ArrayList<String>();
    if (rawText == null || rawText.trim().isEmpty()) {
      return values;
    }
    String text = rawText.trim();
    int depth = 0;
    boolean quoted = false;
    boolean escaped = false;
    int start = -1;
    for (int i = 0; i < text.length(); i++) {
      char ch = text.charAt(i);
      if (escaped) {
        escaped = false;
        continue;
      }
      if (ch == '\\') {
        escaped = true;
        continue;
      }
      if (ch == '"') {
        quoted = !quoted;
        continue;
      }
      if (quoted) {
        continue;
      }
      if (ch == '{') {
        if (depth == 0) {
          start = i;
        }
        depth++;
      } else if (ch == '}') {
        depth--;
        if (depth == 0 && start >= 0) {
          values.add(text.substring(start, i + 1));
          start = -1;
        }
      }
    }
    if (values.isEmpty()) {
      for (String line : text.split("\\r?\\n")) {
        if (!line.trim().isEmpty()) {
          values.add(line.trim());
        }
      }
    }
    return values;
  }

  private static String stringValue(Object value) {
    return value == null ? "" : String.valueOf(value);
  }
}
