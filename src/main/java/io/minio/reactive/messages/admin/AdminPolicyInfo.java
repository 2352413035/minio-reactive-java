package io.minio.reactive.messages.admin;

import com.fasterxml.jackson.databind.JsonNode;
import io.minio.reactive.util.JsonSupport;

/** 管理端策略信息响应。 */
public final class AdminPolicyInfo extends AdminJsonResult {
  private final String policyName;
  private final String policyJson;
  private final String createDate;
  private final String updateDate;

  private AdminPolicyInfo(
      String rawJson, String policyName, String policyJson, String createDate, String updateDate) {
    super(rawJson, JsonSupport.parseMap(rawJson));
    this.policyName = policyName;
    this.policyJson = policyJson;
    this.createDate = createDate;
    this.updateDate = updateDate;
  }

  public static AdminPolicyInfo parse(String rawJson) {
    JsonNode root = JsonSupport.parseTree(rawJson);
    JsonNode policy = root.get("Policy");
    if (policy == null) {
      policy = root.get("policy");
    }
    return new AdminPolicyInfo(
        rawJson,
        JsonSupport.text(root, "PolicyName"),
        policy == null ? rawJson : policy.toString(),
        JsonSupport.text(root, "CreateDate"),
        JsonSupport.text(root, "UpdateDate"));
  }

  public String policyName() {
    return policyName;
  }

  public String policyJson() {
    return policyJson;
  }

  public String createDate() {
    return createDate;
  }

  public String updateDate() {
    return updateDate;
  }
}
