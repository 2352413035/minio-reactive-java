package io.minio.reactive.messages.sts;

import java.util.LinkedHashMap;
import java.util.Map;

/** 使用客户端证书申请临时凭证的请求对象。 */
public final class AssumeRoleWithCertificateRequest {
  private final Integer durationSeconds;

  private AssumeRoleWithCertificateRequest(Integer durationSeconds) {
    if (durationSeconds != null && durationSeconds.intValue() <= 0) {
      throw new IllegalArgumentException("durationSeconds 必须大于 0");
    }
    this.durationSeconds = durationSeconds;
  }

  public static AssumeRoleWithCertificateRequest create() {
    return new AssumeRoleWithCertificateRequest(null);
  }

  public AssumeRoleWithCertificateRequest withDurationSeconds(int durationSeconds) {
    return new AssumeRoleWithCertificateRequest(Integer.valueOf(durationSeconds));
  }

  public Map<String, String> toQueryParameters() {
    Map<String, String> query = new LinkedHashMap<String, String>();
    query.put("Action", "AssumeRoleWithCertificate");
    query.put("Version", "2011-06-15");
    if (durationSeconds != null) {
      query.put("DurationSeconds", String.valueOf(durationSeconds));
    }
    return query;
  }
}
