package io.minio.reactive.messages.admin;

import com.fasterxml.jackson.databind.JsonNode;
import io.minio.reactive.util.JsonSupport;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/** 管理端用户列表响应。 */
public final class AdminUserList extends AdminJsonResult {
  private final Map<String, AdminUserInfo> users;

  private AdminUserList(String rawJson, Map<String, AdminUserInfo> users) {
    super(rawJson, JsonSupport.parseMap(rawJson));
    this.users = Collections.unmodifiableMap(new LinkedHashMap<String, AdminUserInfo>(users));
  }

  public static AdminUserList parse(String rawJson) {
    JsonNode root = JsonSupport.parseTree(rawJson);
    JsonNode source = root.has("users") ? root.get("users") : root;
    Map<String, AdminUserInfo> users = new LinkedHashMap<String, AdminUserInfo>();
    if (source != null && source.isObject()) {
      java.util.Iterator<Map.Entry<String, JsonNode>> fields = source.fields();
      while (fields.hasNext()) {
        Map.Entry<String, JsonNode> entry = fields.next();
        if (entry.getValue().isObject()) {
          users.put(entry.getKey(), AdminUserInfo.parse(entry.getValue().toString()));
        }
      }
    }
    return new AdminUserList(rawJson, users);
  }

  public Map<String, AdminUserInfo> users() {
    return users;
  }
}
