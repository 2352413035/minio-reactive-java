package io.minio.reactive.messages.admin;

import com.fasterxml.jackson.databind.JsonNode;
import io.minio.reactive.util.JsonSupport;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Access key 只读摘要列表。
 *
 * <p>为避免泄漏敏感数据，本模型不保存 raw JSON；解析时只提取 accessKey、状态、父用户、名称、描述、过期时间
 * 以及是否存在 policy。secretKey、sessionToken、私钥等字段不会出现在模型中。
 */
public final class AdminAccessKeySummaryList {
  private final List<AdminAccessKeySummary> serviceAccounts;
  private final List<AdminAccessKeySummary> stsKeys;

  private AdminAccessKeySummaryList(
      List<AdminAccessKeySummary> serviceAccounts, List<AdminAccessKeySummary> stsKeys) {
    this.serviceAccounts =
        Collections.unmodifiableList(new ArrayList<AdminAccessKeySummary>(serviceAccounts));
    this.stsKeys = Collections.unmodifiableList(new ArrayList<AdminAccessKeySummary>(stsKeys));
  }

  public static AdminAccessKeySummaryList parse(String rawJson) {
    JsonNode root = JsonSupport.parseTree(rawJson);
    return new AdminAccessKeySummaryList(
        parseItems(JsonSupport.child(root, "serviceAccounts", "ServiceAccounts", "accessKeys", "AccessKeys")),
        parseItems(JsonSupport.child(root, "stsKeys", "STSKeys", "sts", "STS")));
  }

  private static List<AdminAccessKeySummary> parseItems(JsonNode node) {
    List<AdminAccessKeySummary> result = new ArrayList<AdminAccessKeySummary>();
    if (node == null || node.isNull()) {
      return result;
    }
    if (node.isArray()) {
      for (JsonNode item : node) {
        result.add(AdminAccessKeySummary.parse(item));
      }
      return result;
    }
    if (node.isObject()) {
      result.add(AdminAccessKeySummary.parse(node));
    }
    return result;
  }

  public List<AdminAccessKeySummary> serviceAccounts() {
    return serviceAccounts;
  }

  public List<AdminAccessKeySummary> stsKeys() {
    return stsKeys;
  }

  public int totalCount() {
    return serviceAccounts.size() + stsKeys.size();
  }
}
