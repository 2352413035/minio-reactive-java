package io.minio.reactive.messages.admin;

import com.fasterxml.jackson.databind.JsonNode;
import io.minio.reactive.util.JsonSupport;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

/** IDP 配置名称列表摘要。 */
public final class AdminIdpConfigList extends AdminJsonResult {
  private final String type;
  private final List<String> names;

  private AdminIdpConfigList(String type, String rawJson, List<String> names) {
    super(rawJson, safeMap(rawJson));
    this.type = type == null ? "" : type;
    this.names = Collections.unmodifiableList(new ArrayList<String>(names));
  }

  public static AdminIdpConfigList parse(String type, String rawJson) {
    JsonNode root = JsonSupport.parseTree(rawJson);
    List<String> names = new ArrayList<String>();
    collectNames(root, names);
    return new AdminIdpConfigList(type, rawJson, names);
  }

  public String type() {
    return type;
  }

  public List<String> names() {
    return names;
  }

  public int count() {
    return names.size();
  }

  private static void collectNames(JsonNode node, List<String> names) {
    if (node == null || node.isNull()) {
      return;
    }
    if (node.isArray()) {
      for (JsonNode item : node) {
        collectNames(item, names);
      }
      return;
    }
    if (node.isObject()) {
      String name = JsonSupport.textAny(node, "name", "Name", "id", "ID");
      if (!name.isEmpty()) {
        names.add(name);
        return;
      }
      Iterator<String> fields = node.fieldNames();
      while (fields.hasNext()) {
        names.add(fields.next());
      }
      return;
    }
    String value = node.asText();
    if (value != null && !value.trim().isEmpty()) {
      names.add(value.trim());
    }
  }

  private static java.util.Map<String, Object> safeMap(String rawJson) {
    JsonNode root = JsonSupport.parseTree(rawJson);
    return root != null && root.isObject()
        ? JsonSupport.parseMap(rawJson)
        : java.util.Collections.<String, Object>emptyMap();
  }
}
