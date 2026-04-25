package io.minio.reactive.messages;

import io.minio.reactive.util.JsonSupport;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * FanOut 上传中的单个目标对象描述。
 *
 * <p>MinIO FanOut 会把同一份上传内容写入多个对象。每个 entry 至少需要 key，
 * 也可以带 tags、用户元数据和 contentType 等对象级属性。
 */
public final class PutObjectFanOutEntry {
  private final String key;
  private final Map<String, String> userMetadata;
  private final Map<String, String> tags;
  private final String contentType;

  private PutObjectFanOutEntry(
      String key, Map<String, String> userMetadata, Map<String, String> tags, String contentType) {
    this.key = requireNonBlank(key, "fanout key 不能为空");
    this.userMetadata = copyMap(userMetadata);
    this.tags = copyMap(tags);
    this.contentType = emptyToNull(contentType);
  }

  public static PutObjectFanOutEntry of(String key) {
    return new PutObjectFanOutEntry(key, null, null, null);
  }

  public PutObjectFanOutEntry withUserMetadata(Map<String, String> userMetadata) {
    return new PutObjectFanOutEntry(key, userMetadata, tags, contentType);
  }

  public PutObjectFanOutEntry withTags(Map<String, String> tags) {
    return new PutObjectFanOutEntry(key, userMetadata, tags, contentType);
  }

  public PutObjectFanOutEntry withContentType(String contentType) {
    return new PutObjectFanOutEntry(key, userMetadata, tags, contentType);
  }

  public String key() {
    return key;
  }

  public Map<String, String> userMetadata() {
    return userMetadata;
  }

  public Map<String, String> tags() {
    return tags;
  }

  public String contentType() {
    return contentType;
  }

  /** 生成 MinIO `x-minio-fanout-list` 表单字段需要的 JSON 对象。 */
  public String toJson() {
    Map<String, Object> value = new LinkedHashMap<String, Object>();
    value.put("key", key);
    if (!userMetadata.isEmpty()) {
      value.put("metadata", userMetadata);
    }
    if (!tags.isEmpty()) {
      value.put("tags", tags);
    }
    if (contentType != null) {
      value.put("contentType", contentType);
    }
    return JsonSupport.toJsonString(value);
  }

  public static String toFanOutList(List<PutObjectFanOutEntry> entries) {
    if (entries == null || entries.isEmpty()) {
      throw new IllegalArgumentException("fanout entry 列表不能为空");
    }
    StringBuilder builder = new StringBuilder();
    for (PutObjectFanOutEntry entry : entries) {
      if (entry == null) {
        throw new IllegalArgumentException("fanout entry 不能为空");
      }
      builder.append(entry.toJson());
    }
    return builder.toString();
  }

  private static Map<String, String> copyMap(Map<String, String> value) {
    if (value == null || value.isEmpty()) {
      return Collections.emptyMap();
    }
    return Collections.unmodifiableMap(new LinkedHashMap<String, String>(value));
  }

  private static String requireNonBlank(String value, String message) {
    if (value == null || value.trim().isEmpty()) {
      throw new IllegalArgumentException(message);
    }
    return value;
  }

  private static String emptyToNull(String value) {
    return value == null || value.trim().isEmpty() ? null : value;
  }
}
