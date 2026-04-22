package io.minio.reactive.messages.admin;

import io.minio.reactive.util.JsonSupport;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/** 管理端策略列表响应。 */
public final class AdminPolicyList extends AdminJsonResult {
  private final Map<String, Object> policies;

  private AdminPolicyList(String rawJson, Map<String, Object> policies) {
    super(rawJson, policies);
    this.policies = Collections.unmodifiableMap(new LinkedHashMap<String, Object>(policies));
  }

  public static AdminPolicyList parse(String rawJson) {
    return new AdminPolicyList(rawJson, JsonSupport.parseMap(rawJson));
  }

  public Map<String, Object> policies() {
    return policies;
  }
}
