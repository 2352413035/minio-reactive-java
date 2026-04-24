package io.minio.reactive.messages.sts;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

/** 使用 SSO 表单入口申请临时凭证的请求对象。 */
public final class AssumeRoleSsoRequest {
  private final String action;
  private final String token;
  private final String roleArn;
  private final Integer durationSeconds;

  private AssumeRoleSsoRequest(String action, String token, String roleArn, Integer durationSeconds) {
    if (action == null || action.trim().isEmpty()) {
      throw new IllegalArgumentException("action 不能为空");
    }
    if (token == null || token.trim().isEmpty()) {
      throw new IllegalArgumentException("token 不能为空");
    }
    if (durationSeconds != null && durationSeconds.intValue() <= 0) {
      throw new IllegalArgumentException("durationSeconds 必须大于 0");
    }
    this.action = action.trim();
    this.token = token.trim();
    this.roleArn = roleArn == null ? "" : roleArn.trim();
    this.durationSeconds = durationSeconds;
  }

  public static AssumeRoleSsoRequest webIdentity(String token) {
    return new AssumeRoleSsoRequest("AssumeRoleWithWebIdentity", token, "", null);
  }

  public static AssumeRoleSsoRequest clientGrants(String token) {
    return new AssumeRoleSsoRequest("AssumeRoleWithClientGrants", token, "", null);
  }

  public AssumeRoleSsoRequest withRoleArn(String roleArn) {
    return new AssumeRoleSsoRequest(action, token, roleArn, durationSeconds);
  }

  public AssumeRoleSsoRequest withDurationSeconds(int durationSeconds) {
    return new AssumeRoleSsoRequest(action, token, roleArn, Integer.valueOf(durationSeconds));
  }

  public byte[] toFormBytes() {
    Map<String, String> form = new LinkedHashMap<String, String>();
    form.put("Action", action);
    form.put("Version", "2011-06-15");
    if ("AssumeRoleWithWebIdentity".equals(action)) {
      form.put("WebIdentityToken", token);
    } else {
      form.put("Token", token);
    }
    if (!roleArn.isEmpty()) {
      form.put("RoleArn", roleArn);
    }
    if (durationSeconds != null) {
      form.put("DurationSeconds", String.valueOf(durationSeconds));
    }
    return formEncode(form).getBytes(StandardCharsets.UTF_8);
  }

  private static String formEncode(Map<String, String> values) {
    StringBuilder builder = new StringBuilder();
    for (Map.Entry<String, String> entry : values.entrySet()) {
      if (builder.length() > 0) {
        builder.append('&');
      }
      builder.append(urlEncode(entry.getKey())).append('=').append(urlEncode(entry.getValue()));
    }
    return builder.toString();
  }

  private static String urlEncode(String value) {
    try {
      return URLEncoder.encode(value, "UTF-8");
    } catch (UnsupportedEncodingException e) {
      throw new IllegalStateException("UTF-8 编码不可用", e);
    }
  }
}
