package io.minio.reactive.messages.admin;

import java.util.LinkedHashMap;
import java.util.Map;

/** 新增或更新内部用户的请求对象。 */
public final class AddUserRequest {
  private final String accessKey;
  private final String secretKey;
  private final String policy;
  private final String status;

  private AddUserRequest(String accessKey, String secretKey, String policy, String status) {
    if (accessKey == null || accessKey.trim().isEmpty()) {
      throw new IllegalArgumentException("accessKey 不能为空");
    }
    if (secretKey == null || secretKey.trim().isEmpty()) {
      throw new IllegalArgumentException("secretKey 不能为空");
    }
    this.accessKey = accessKey;
    this.secretKey = secretKey;
    this.policy = policy == null ? "" : policy;
    this.status = status == null || status.trim().isEmpty() ? "enabled" : status;
  }

  public static AddUserRequest of(String accessKey, String secretKey) {
    return new AddUserRequest(accessKey, secretKey, "", "enabled");
  }

  public static AddUserRequest of(String accessKey, String secretKey, String policy, String status) {
    return new AddUserRequest(accessKey, secretKey, policy, status);
  }

  public String accessKey() {
    return accessKey;
  }

  public Map<String, Object> toPayload() {
    Map<String, Object> payload = new LinkedHashMap<String, Object>();
    payload.put("secretKey", secretKey);
    if (!policy.isEmpty()) {
      payload.put("policy", policy);
    }
    payload.put("status", status);
    return payload;
  }
}
