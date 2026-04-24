package io.minio.reactive.util;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
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
      JsonNode root = MAPPER.readTree(json);
      if (root == null || root.isNull()) {
        return Collections.emptyMap();
      }
      if (root.isObject()) {
        return MAPPER.convertValue(root, MAP_TYPE);
      }
      Map<String, Object> wrapped = new LinkedHashMap<String, Object>();
      // 部分 MinIO Admin 接口顶层直接返回数组；统一包进 items，避免通用 JSON 包装误报解析失败。
      wrapped.put("items", MAPPER.convertValue(root, Object.class));
      return wrapped;
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

  /**
   * 按多个候选字段名读取子节点。
   *
   * <p>MinIO Admin 响应里有 Go 结构体默认字段名（如 `Backend`）和 json tag 字段名（如
   * `objectsCount`）两种风格。强类型模型优先使用这个方法做容错，避免因为大小写漂移导致摘要字段误读为空。
   */
  public static JsonNode child(JsonNode node, String... fields) {
    if (node == null || fields == null) {
      return null;
    }
    for (String field : fields) {
      JsonNode value = node.get(field);
      if (value != null && !value.isNull()) {
        return value;
      }
    }
    return null;
  }

  /** 从多个候选字段中读取文本值。 */
  public static String textAny(JsonNode node, String... fields) {
    JsonNode value = child(node, fields);
    return value == null || value.isNull() ? "" : value.asText();
  }

  /** 从多个候选字段中读取 long 值。 */
  public static long longAny(JsonNode node, String... fields) {
    JsonNode value = child(node, fields);
    return value == null || value.isNull() ? 0L : value.asLong();
  }

  /** 从多个候选字段中读取 int 值。 */
  public static int intAny(JsonNode node, String... fields) {
    JsonNode value = child(node, fields);
    return value == null || value.isNull() ? 0 : value.asInt();
  }

  /** 从多个候选字段中读取 boolean 值。 */
  public static boolean booleanAny(JsonNode node, String... fields) {
    JsonNode value = child(node, fields);
    return value != null && !value.isNull() && value.asBoolean();
  }

  /** 统计数组元素数或对象字段数，未知结构按 0 处理。 */
  public static int nodeSize(JsonNode node) {
    if (node == null || node.isNull()) {
      return 0;
    }
    if (node.isArray() || node.isObject()) {
      return node.size();
    }
    return 0;
  }

  /** 汇总 JSON object 中的数值字段，用于统计 MinIO `BackendDisks` 这类 map 响应。 */
  public static int sumNumericObjectValues(JsonNode node) {
    if (node == null || !node.isObject()) {
      return 0;
    }
    int sum = 0;
    Iterator<JsonNode> values = node.elements();
    while (values.hasNext()) {
      sum += values.next().asInt();
    }
    return sum;
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
