package io.minio.reactive.messages.sts;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

/** 使用已签名 MinIO/AWS 凭证申请 STS AssumeRole 临时凭证的请求对象。 */
public final class AssumeRoleRequest {
  private final Integer durationSeconds;
  private final String policy;
  private final String tokenRevokeType;

  private AssumeRoleRequest(Builder builder) {
    if (builder.durationSeconds != null && builder.durationSeconds.intValue() <= 0) {
      throw new IllegalArgumentException("durationSeconds 必须大于 0");
    }
    this.durationSeconds = builder.durationSeconds;
    this.policy = builder.policy == null ? "" : builder.policy;
    this.tokenRevokeType = builder.tokenRevokeType == null ? "" : builder.tokenRevokeType;
  }

  public static Builder builder() { return new Builder(); }

  public Integer durationSeconds() { return durationSeconds; }

  public String policy() { return policy; }

  public String tokenRevokeType() { return tokenRevokeType; }

  /** 生成 STS 表单请求体，Action/Version 固定对齐 MinIO STS 处理器。 */
  public byte[] toFormBytes() {
    Map<String, String> form = new LinkedHashMap<String, String>();
    form.put("Action", "AssumeRole");
    form.put("Version", "2011-06-15");
    if (durationSeconds != null) {
      form.put("DurationSeconds", String.valueOf(durationSeconds));
    }
    if (!policy.isEmpty()) {
      form.put("Policy", policy);
    }
    if (!tokenRevokeType.isEmpty()) {
      form.put("TokenRevokeType", tokenRevokeType);
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

  public static final class Builder {
    private Integer durationSeconds;
    private String policy;
    private String tokenRevokeType;

    private Builder() {}

    public Builder durationSeconds(int durationSeconds) {
      this.durationSeconds = Integer.valueOf(durationSeconds);
      return this;
    }

    public Builder policy(String policy) {
      this.policy = policy;
      return this;
    }

    public Builder tokenRevokeType(String tokenRevokeType) {
      this.tokenRevokeType = tokenRevokeType;
      return this;
    }

    public AssumeRoleRequest build() {
      return new AssumeRoleRequest(this);
    }
  }
}
