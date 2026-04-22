package io.minio.reactive.messages.admin;

import io.minio.reactive.util.JsonSupport;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/** 管理端 JSON 响应的通用包装，保留原文和 Map 形式，避免字段漂移时丢失信息。 */
public class AdminJsonResult {
  private final String rawJson;
  private final Map<String, Object> values;

  public AdminJsonResult(String rawJson, Map<String, Object> values) {
    this.rawJson = rawJson == null ? "" : rawJson;
    this.values = Collections.unmodifiableMap(new LinkedHashMap<String, Object>(values));
  }

  public static AdminJsonResult parse(String rawJson) {
    return new AdminJsonResult(rawJson, JsonSupport.parseMap(rawJson));
  }

  public String rawJson() {
    return rawJson;
  }

  public Map<String, Object> values() {
    return values;
  }
}
