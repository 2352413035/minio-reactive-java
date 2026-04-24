package io.minio.reactive.messages.admin;

import com.fasterxml.jackson.databind.JsonNode;
import io.minio.reactive.util.JsonSupport;

/**
 * Access key 只读摘要。
 *
 * <p>该模型故意不保留 raw JSON，也不暴露 secretKey、sessionToken、私钥等敏感字段。
 * 如果服务端响应里包含这些字段，解析时会直接丢弃，只留下排障所需的非敏感元数据。
 */
public final class AdminAccessKeySummary {
  private final String accessKey;
  private final String parentUser;
  private final String accountStatus;
  private final String name;
  private final String description;
  private final String expiration;
  private final boolean hasPolicy;

  private AdminAccessKeySummary(
      String accessKey,
      String parentUser,
      String accountStatus,
      String name,
      String description,
      String expiration,
      boolean hasPolicy) {
    this.accessKey = accessKey;
    this.parentUser = parentUser;
    this.accountStatus = accountStatus;
    this.name = name;
    this.description = description;
    this.expiration = expiration;
    this.hasPolicy = hasPolicy;
  }

  static AdminAccessKeySummary parse(JsonNode node) {
    String policy = JsonSupport.textAny(node, "policy", "Policy");
    return new AdminAccessKeySummary(
        JsonSupport.textAny(node, "accessKey", "AccessKey"),
        JsonSupport.textAny(node, "parentUser", "ParentUser"),
        JsonSupport.textAny(node, "accountStatus", "AccountStatus", "status", "Status"),
        JsonSupport.textAny(node, "name", "Name"),
        JsonSupport.textAny(node, "description", "Description"),
        JsonSupport.textAny(node, "expiration", "Expiration"),
        policy != null && !policy.trim().isEmpty());
  }

  public String accessKey() {
    return accessKey;
  }

  public String parentUser() {
    return parentUser;
  }

  public String accountStatus() {
    return accountStatus;
  }

  public String name() {
    return name;
  }

  public String description() {
    return description;
  }

  public String expiration() {
    return expiration;
  }

  public boolean hasPolicy() {
    return hasPolicy;
  }
}
