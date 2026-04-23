package io.minio.reactive.messages.admin;

import com.fasterxml.jackson.databind.JsonNode;
import io.minio.reactive.util.JsonSupport;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** 管理端用户组列表响应。 */
public final class AdminGroupList extends AdminJsonResult {
  private final List<String> groups;

  private AdminGroupList(String rawJson, List<String> groups) {
    super(rawJson, rawJson != null && rawJson.trim().startsWith("{") ? JsonSupport.parseMap(rawJson) : Collections.<String, Object>emptyMap());
    this.groups = Collections.unmodifiableList(new ArrayList<String>(groups));
  }

  public static AdminGroupList parse(String rawJson) {
    JsonNode root = JsonSupport.parseTree(rawJson);
    JsonNode source = root.has("groups") ? root.get("groups") : root;
    List<String> groups = new ArrayList<String>();
    if (source != null && source.isArray()) {
      for (JsonNode group : source) {
        groups.add(group.asText());
      }
    }
    return new AdminGroupList(rawJson, groups);
  }

  public List<String> groups() {
    return groups;
  }
}
