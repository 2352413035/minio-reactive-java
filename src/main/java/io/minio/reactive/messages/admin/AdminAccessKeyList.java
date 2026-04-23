package io.minio.reactive.messages.admin;

import com.fasterxml.jackson.databind.JsonNode;
import io.minio.reactive.util.JsonSupport;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Access key 列表响应。 */
public final class AdminAccessKeyList extends AdminJsonResult {
  private final List<AdminAccessKeyInfo> serviceAccounts;
  private final List<AdminAccessKeyInfo> stsKeys;

  private AdminAccessKeyList(
      String rawJson, List<AdminAccessKeyInfo> serviceAccounts, List<AdminAccessKeyInfo> stsKeys) {
    super(rawJson, JsonSupport.parseMap(rawJson));
    this.serviceAccounts =
        Collections.unmodifiableList(new ArrayList<AdminAccessKeyInfo>(serviceAccounts));
    this.stsKeys = Collections.unmodifiableList(new ArrayList<AdminAccessKeyInfo>(stsKeys));
  }

  public static AdminAccessKeyList parse(String rawJson) {
    JsonNode root = JsonSupport.parseTree(rawJson);
    return new AdminAccessKeyList(
        rawJson, parseItems(root.get("serviceAccounts")), parseItems(root.get("stsKeys")));
  }

  private static List<AdminAccessKeyInfo> parseItems(JsonNode source) {
    List<AdminAccessKeyInfo> result = new ArrayList<AdminAccessKeyInfo>();
    if (source != null && source.isArray()) {
      for (JsonNode item : source) {
        result.add(AdminAccessKeyInfo.parse(item.toString()));
      }
    }
    return result;
  }

  public List<AdminAccessKeyInfo> serviceAccounts() {
    return serviceAccounts;
  }

  public List<AdminAccessKeyInfo> stsKeys() {
    return stsKeys;
  }
}
