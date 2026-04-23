package io.minio.reactive.messages.admin;

import com.fasterxml.jackson.databind.JsonNode;
import io.minio.reactive.util.JsonSupport;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** 服务账号列表响应。 */
public final class ServiceAccountList extends AdminJsonResult {
  private final List<ServiceAccountInfo> accounts;

  private ServiceAccountList(String rawJson, List<ServiceAccountInfo> accounts) {
    super(rawJson, JsonSupport.parseMap(rawJson));
    this.accounts = Collections.unmodifiableList(new ArrayList<ServiceAccountInfo>(accounts));
  }

  public static ServiceAccountList parse(String rawJson) {
    JsonNode root = JsonSupport.parseTree(rawJson);
    JsonNode source = root.has("accounts") ? root.get("accounts") : root.get("serviceAccounts");
    List<ServiceAccountInfo> accounts = new ArrayList<ServiceAccountInfo>();
    if (source != null && source.isArray()) {
      for (JsonNode account : source) {
        accounts.add(ServiceAccountInfo.parse(account.toString()));
      }
    }
    return new ServiceAccountList(rawJson, accounts);
  }

  public List<ServiceAccountInfo> accounts() { return accounts; }
}
