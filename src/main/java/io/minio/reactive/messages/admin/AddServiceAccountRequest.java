package io.minio.reactive.messages.admin;

import java.util.LinkedHashMap;
import java.util.Map;

/** 创建服务账号的请求对象。 */
public final class AddServiceAccountRequest {
  private final String targetUser;
  private final String accessKey;
  private final String secretKey;
  private final String name;
  private final String description;
  private final String policyJson;
  private final String expiration;

  private AddServiceAccountRequest(Builder builder) {
    this.targetUser = emptyToBlank(builder.targetUser);
    this.accessKey = emptyToBlank(builder.accessKey);
    this.secretKey = emptyToBlank(builder.secretKey);
    this.name = emptyToBlank(builder.name);
    this.description = emptyToBlank(builder.description);
    this.policyJson = emptyToBlank(builder.policyJson);
    this.expiration = emptyToBlank(builder.expiration);
  }

  public static Builder builder() {
    return new Builder();
  }

  public Map<String, Object> toPayload() {
    Map<String, Object> payload = new LinkedHashMap<String, Object>();
    putIfNotEmpty(payload, "targetUser", targetUser);
    putIfNotEmpty(payload, "accessKey", accessKey);
    putIfNotEmpty(payload, "secretKey", secretKey);
    putIfNotEmpty(payload, "name", name);
    putIfNotEmpty(payload, "description", description);
    putIfNotEmpty(payload, "policy", policyJson);
    putIfNotEmpty(payload, "expiration", expiration);
    return payload;
  }

  private static void putIfNotEmpty(Map<String, Object> payload, String key, String value) {
    if (value != null && !value.isEmpty()) {
      payload.put(key, value);
    }
  }

  private static String emptyToBlank(String value) {
    return value == null ? "" : value;
  }

  public static final class Builder {
    private String targetUser;
    private String accessKey;
    private String secretKey;
    private String name;
    private String description;
    private String policyJson;
    private String expiration;

    private Builder() {}

    public Builder targetUser(String targetUser) {
      this.targetUser = targetUser;
      return this;
    }

    public Builder accessKey(String accessKey) {
      this.accessKey = accessKey;
      return this;
    }

    public Builder secretKey(String secretKey) {
      this.secretKey = secretKey;
      return this;
    }

    public Builder name(String name) {
      this.name = name;
      return this;
    }

    public Builder description(String description) {
      this.description = description;
      return this;
    }

    public Builder policyJson(String policyJson) {
      this.policyJson = policyJson;
      return this;
    }

    public Builder expiration(String expiration) {
      this.expiration = expiration;
      return this;
    }

    public AddServiceAccountRequest build() {
      return new AddServiceAccountRequest(this);
    }
  }
}
