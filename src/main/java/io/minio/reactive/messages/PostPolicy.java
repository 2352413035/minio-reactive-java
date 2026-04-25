package io.minio.reactive.messages;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * 预签名 POST 上传使用的策略模型。
 *
 * <p>它对齐 minio-java 的 PostPolicy 使用方式：调用方先声明 bucket、过期时间、key 条件、
 * content-type 条件或文件大小范围，SDK 再基于当前凭证生成 form-data。
 */
public final class PostPolicy {
  private static final List<String> RESERVED_ELEMENTS =
      Arrays.asList(
          "bucket",
          "x-amz-algorithm",
          "x-amz-credential",
          "x-amz-date",
          "policy",
          "x-amz-signature");
  private static final DateTimeFormatter ISO8601_UTC =
      DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH':'mm':'ss'.'SSS'Z'", Locale.US)
          .withZone(ZoneOffset.UTC);

  private final String bucket;
  private final ZonedDateTime expiration;
  private final Map<String, String> equalsConditions = new LinkedHashMap<String, String>();
  private final Map<String, String> startsWithConditions = new LinkedHashMap<String, String>();
  private Long lowerLimit;
  private Long upperLimit;

  public PostPolicy(String bucket, ZonedDateTime expiration) {
    this.bucket = requireNonBlank(bucket, "bucket 不能为空");
    if (expiration == null) {
      throw new IllegalArgumentException("expiration 不能为空");
    }
    this.expiration = expiration.withZoneSameInstant(ZoneOffset.UTC);
  }

  public static PostPolicy of(String bucket, ZonedDateTime expiration) {
    return new PostPolicy(bucket, expiration);
  }

  public String bucket() {
    return bucket;
  }

  public ZonedDateTime expiration() {
    return expiration;
  }

  public void addEqualsCondition(String element, String value) {
    String normalized = normalizeConditionElement(element);
    if ("success_action_redirect".equals(normalized)
        || "redirect".equals(normalized)
        || "content-length-range".equals(normalized)) {
      throw new IllegalArgumentException(normalized + " 不支持 equals 条件");
    }
    rejectReserved(normalized);
    equalsConditions.put(normalized, value == null ? "" : value);
  }

  public void removeEqualsCondition(String element) {
    equalsConditions.remove(normalizeConditionElement(element));
  }

  public void addStartsWithCondition(String element, String value) {
    String normalized = normalizeConditionElement(element);
    if ("success_action_status".equals(normalized)
        || "content-length-range".equals(normalized)
        || (normalized.startsWith("x-amz-") && !normalized.startsWith("x-amz-meta-"))) {
      throw new IllegalArgumentException(normalized + " 不支持 starts-with 条件");
    }
    rejectReserved(normalized);
    startsWithConditions.put(normalized, value == null ? "" : value);
  }

  public void removeStartsWithCondition(String element) {
    startsWithConditions.remove(normalizeConditionElement(element));
  }

  public void addContentLengthRangeCondition(long lowerLimit, long upperLimit) {
    if (lowerLimit < 0L || upperLimit < 0L) {
      throw new IllegalArgumentException("content-length-range 不能为负数");
    }
    if (lowerLimit > upperLimit) {
      throw new IllegalArgumentException("content-length-range 下限不能大于上限");
    }
    this.lowerLimit = lowerLimit;
    this.upperLimit = upperLimit;
  }

  public void removeContentLengthRangeCondition() {
    this.lowerLimit = null;
    this.upperLimit = null;
  }

  /**
   * 构造待 Base64 编码和签名的 JSON policy。
   *
   * <p>签名字段也必须加入 policy 条件，否则浏览器表单上传时服务端会拒绝验签。
   */
  public String policyDocument(
      String algorithm, String credential, String amzDate, String sessionToken) {
    if (!equalsConditions.containsKey("key") && !startsWithConditions.containsKey("key")) {
      throw new IllegalArgumentException("POST policy 必须包含 key 条件");
    }
    StringBuilder builder = new StringBuilder();
    builder.append('{');
    appendJsonField(builder, "expiration", ISO8601_UTC.format(expiration));
    builder.append(",\"conditions\":[");
    int index = 0;
    index = appendCondition(builder, index, "eq", "bucket", bucket);
    for (Map.Entry<String, String> entry : equalsConditions.entrySet()) {
      index = appendCondition(builder, index, "eq", entry.getKey(), entry.getValue());
    }
    for (Map.Entry<String, String> entry : startsWithConditions.entrySet()) {
      index = appendCondition(builder, index, "starts-with", entry.getKey(), entry.getValue());
    }
    if (lowerLimit != null && upperLimit != null) {
      if (index++ > 0) {
        builder.append(',');
      }
      builder.append("[\"content-length-range\",")
          .append(lowerLimit)
          .append(',')
          .append(upperLimit)
          .append(']');
    }
    index = appendCondition(builder, index, "eq", "x-amz-algorithm", algorithm);
    index = appendCondition(builder, index, "eq", "x-amz-credential", credential);
    if (sessionToken != null && !sessionToken.trim().isEmpty()) {
      index = appendCondition(builder, index, "eq", "x-amz-security-token", sessionToken);
    }
    appendCondition(builder, index, "eq", "x-amz-date", amzDate);
    builder.append("]}");
    return builder.toString();
  }

  private static int appendCondition(
      StringBuilder builder, int index, String operator, String element, String value) {
    if (index > 0) {
      builder.append(',');
    }
    builder
        .append("[\"")
        .append(escapeJson(operator))
        .append("\",\"$")
        .append(escapeJson(element))
        .append("\",\"")
        .append(escapeJson(value))
        .append("\"]");
    return index + 1;
  }

  private static void appendJsonField(StringBuilder builder, String key, String value) {
    builder
        .append('"')
        .append(escapeJson(key))
        .append("\":\"")
        .append(escapeJson(value))
        .append('"');
  }

  private static String normalizeConditionElement(String element) {
    String normalized = requireNonBlank(element, "condition element 不能为空");
    return normalized.startsWith("$") ? normalized.substring(1) : normalized;
  }

  private static void rejectReserved(String element) {
    if (RESERVED_ELEMENTS.contains(element)) {
      throw new IllegalArgumentException(element + " 是 SDK 自动生成字段，不能手动设置");
    }
  }

  private static String requireNonBlank(String value, String message) {
    if (value == null || value.trim().isEmpty()) {
      throw new IllegalArgumentException(message);
    }
    return value;
  }

  private static String escapeJson(String value) {
    if (value == null) {
      return "";
    }
    StringBuilder builder = new StringBuilder(value.length());
    for (int i = 0; i < value.length(); i++) {
      char ch = value.charAt(i);
      switch (ch) {
        case '"':
          builder.append("\\\"");
          break;
        case '\\':
          builder.append("\\\\");
          break;
        case '\b':
          builder.append("\\b");
          break;
        case '\f':
          builder.append("\\f");
          break;
        case '\n':
          builder.append("\\n");
          break;
        case '\r':
          builder.append("\\r");
          break;
        case '\t':
          builder.append("\\t");
          break;
        default:
          if (ch < 0x20) {
            builder.append(String.format("\\u%04x", (int) ch));
          } else {
            builder.append(ch);
          }
      }
    }
    return builder.toString();
  }
}
