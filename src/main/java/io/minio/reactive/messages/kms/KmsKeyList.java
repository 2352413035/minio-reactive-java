package io.minio.reactive.messages.kms;

import com.fasterxml.jackson.databind.JsonNode;
import io.minio.reactive.util.JsonSupport;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** KMS key 列表响应。 */
public final class KmsKeyList extends KmsJsonResult {
  private final List<String> keys;

  private KmsKeyList(String rawJson, List<String> keys) {
    super(rawJson, JsonSupport.parseMap(rawJson));
    this.keys = Collections.unmodifiableList(new ArrayList<String>(keys));
  }

  public static KmsKeyList parse(String rawJson) {
    JsonNode root = JsonSupport.parseTree(rawJson);
    List<String> keys = new ArrayList<String>();
    JsonNode names = root.get("keys");
    if (names != null && names.isArray()) {
      for (JsonNode name : names) {
        keys.add(name.asText());
      }
    }
    return new KmsKeyList(rawJson, keys);
  }

  public List<String> keys() {
    return keys;
  }
}
