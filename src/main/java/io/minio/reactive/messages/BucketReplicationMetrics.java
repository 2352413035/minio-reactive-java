package io.minio.reactive.messages;

import io.minio.reactive.util.JsonSupport;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/** bucket replication metrics JSON 响应包装。 */
public final class BucketReplicationMetrics {
  private final String version;
  private final String rawJson;
  private final Map<String, Object> values;

  public BucketReplicationMetrics(String version, String rawJson, Map<String, Object> values) {
    this.version = version == null ? "" : version;
    this.rawJson = rawJson == null ? "" : rawJson;
    this.values =
        values == null
            ? Collections.<String, Object>emptyMap()
            : Collections.unmodifiableMap(new LinkedHashMap<String, Object>(values));
  }

  public static BucketReplicationMetrics parse(String version, String rawJson) {
    return new BucketReplicationMetrics(version, rawJson, JsonSupport.parseMap(rawJson));
  }

  public String version() {
    return version;
  }

  public String rawJson() {
    return rawJson;
  }

  public Map<String, Object> values() {
    return values;
  }

  public long uptime() {
    Object value = values.get("uptime");
    return value instanceof Number ? ((Number) value).longValue() : 0L;
  }
}
