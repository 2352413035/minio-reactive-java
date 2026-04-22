package io.minio.reactive.util;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Map;

/**
 * JSON 编解码工具。
 *
 * <p>项目已经依赖 Jackson，这里集中封装 ObjectMapper，避免各个客户端重复创建解析器。
 */
public final class JsonSupport {
  private static final ObjectMapper MAPPER = new ObjectMapper();
  private static final TypeReference<Map<String, Object>> MAP_TYPE =
      new TypeReference<Map<String, Object>>() {};

  private JsonSupport() {}

  public static JsonNode parseTree(String json) {
    try {
      return MAPPER.readTree(json == null ? "{}" : json);
    } catch (Exception e) {
      throw new IllegalArgumentException("无法解析 MinIO JSON 响应", e);
    }
  }

  public static Map<String, Object> parseMap(String json) {
    if (json == null || json.trim().isEmpty()) {
      return Collections.emptyMap();
    }
    try {
      return MAPPER.readValue(json, MAP_TYPE);
    } catch (Exception e) {
      throw new IllegalArgumentException("无法解析 MinIO JSON 响应", e);
    }
  }

  public static byte[] toJsonBytes(Object value) {
    try {
      return MAPPER.writeValueAsBytes(value);
    } catch (Exception e) {
      throw new IllegalArgumentException("无法生成 MinIO JSON 请求体", e);
    }
  }

  public static String toJsonString(Object value) {
    return new String(toJsonBytes(value), StandardCharsets.UTF_8);
  }

  public static String text(JsonNode node, String field) {
    JsonNode value = node == null ? null : node.get(field);
    return value == null || value.isNull() ? "" : value.asText();
  }

  public static long longValue(JsonNode node, String field) {
    JsonNode value = node == null ? null : node.get(field);
    return value == null || value.isNull() ? 0L : value.asLong();
  }

  public static int intValue(JsonNode node, String field) {
    JsonNode value = node == null ? null : node.get(field);
    return value == null || value.isNull() ? 0 : value.asInt();
  }
}
