package io.minio.reactive.messages.admin;

import com.fasterxml.jackson.databind.JsonNode;
import io.minio.reactive.util.JsonSupport;

/** Access key 信息响应。 */
public final class AdminAccessKeyInfo extends AdminJsonResult {
  private final String parentUser;
  private final String accountStatus;
  private final boolean impliedPolicy;
  private final String accessKey;
  private final String name;
  private final String description;
  private final String expiration;
  private final String policy;

  private AdminAccessKeyInfo(
      String rawJson,
      String parentUser,
      String accountStatus,
      boolean impliedPolicy,
      String accessKey,
      String name,
      String description,
      String expiration,
      String policy) {
    super(rawJson, JsonSupport.parseMap(rawJson));
    this.parentUser = parentUser;
    this.accountStatus = accountStatus;
    this.impliedPolicy = impliedPolicy;
    this.accessKey = accessKey;
    this.name = name;
    this.description = description;
    this.expiration = expiration;
    this.policy = policy;
  }

  public static AdminAccessKeyInfo parse(String rawJson) {
    JsonNode root = JsonSupport.parseTree(rawJson);
    return new AdminAccessKeyInfo(
        rawJson,
        JsonSupport.text(root, "parentUser"),
        JsonSupport.text(root, "accountStatus"),
        root.has("impliedPolicy") && root.get("impliedPolicy").asBoolean(),
        JsonSupport.text(root, "accessKey"),
        JsonSupport.text(root, "name"),
        JsonSupport.text(root, "description"),
        JsonSupport.text(root, "expiration"),
        JsonSupport.text(root, "policy"));
  }

  public String parentUser() {
    return parentUser;
  }

  public String accountStatus() {
    return accountStatus;
  }

  public boolean impliedPolicy() {
    return impliedPolicy;
  }

  public String accessKey() {
    return accessKey;
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

  public String policy() {
    return policy;
  }
}
