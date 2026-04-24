package io.minio.reactive.messages.sts;

import java.util.LinkedHashMap;
import java.util.Map;

/** 使用自定义身份插件 token 申请临时凭证的请求对象。 */
public final class AssumeRoleWithCustomTokenRequest {
  private final String token;
  private final String roleArn;
  private final Integer durationSeconds;

  private AssumeRoleWithCustomTokenRequest(String token, String roleArn, Integer durationSeconds) {
    if (token == null || token.trim().isEmpty()) {
      throw new IllegalArgumentException("token 不能为空");
    }
    if (durationSeconds != null && durationSeconds.intValue() <= 0) {
      throw new IllegalArgumentException("durationSeconds 必须大于 0");
    }
    this.token = token.trim();
    this.roleArn = roleArn == null ? "" : roleArn.trim();
    this.durationSeconds = durationSeconds;
  }

  public static AssumeRoleWithCustomTokenRequest of(String token) {
    return new AssumeRoleWithCustomTokenRequest(token, "", null);
  }

  public AssumeRoleWithCustomTokenRequest withRoleArn(String roleArn) {
    return new AssumeRoleWithCustomTokenRequest(token, roleArn, durationSeconds);
  }

  public AssumeRoleWithCustomTokenRequest withDurationSeconds(int durationSeconds) {
    return new AssumeRoleWithCustomTokenRequest(token, roleArn, Integer.valueOf(durationSeconds));
  }

  public Map<String, String> toQueryParameters() {
    Map<String, String> query = new LinkedHashMap<String, String>();
    query.put("Action", "AssumeRoleWithCustomToken");
    query.put("Version", "2011-06-15");
    query.put("Token", token);
    if (!roleArn.isEmpty()) {
      query.put("RoleArn", roleArn);
    }
    if (durationSeconds != null) {
      query.put("DurationSeconds", String.valueOf(durationSeconds));
    }
    return query;
  }
}
