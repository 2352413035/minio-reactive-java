package io.minio.reactive.messages.kms;

import io.minio.reactive.util.JsonSupport;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/** KMS JSON 响应的通用包装。 */
public class KmsJsonResult {
  private final String rawJson;
  private final Map<String, Object> values;

  public KmsJsonResult(String rawJson, Map<String, Object> values) {
    this.rawJson = rawJson == null ? "" : rawJson;
    this.values = Collections.unmodifiableMap(new LinkedHashMap<String, Object>(values));
  }

  public static KmsJsonResult parse(String rawJson) {
    return new KmsJsonResult(rawJson, JsonSupport.parseMap(rawJson));
  }

  public String rawJson() {
    return rawJson;
  }

  public Map<String, Object> values() {
    return values;
  }
}
