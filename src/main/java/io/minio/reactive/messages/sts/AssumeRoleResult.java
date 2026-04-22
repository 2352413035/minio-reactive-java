package io.minio.reactive.messages.sts;

import io.minio.reactive.credentials.ReactiveCredentials;

/** STS AssumeRole 系列接口返回的临时凭证结果。 */
public final class AssumeRoleResult {
  private final ReactiveCredentials credentials;
  private final String expiration;
  private final String rawXml;

  public AssumeRoleResult(ReactiveCredentials credentials, String expiration, String rawXml) {
    this.credentials = credentials;
    this.expiration = expiration;
    this.rawXml = rawXml == null ? "" : rawXml;
  }

  public ReactiveCredentials credentials() {
    return credentials;
  }

  public String expiration() {
    return expiration;
  }

  public String rawXml() {
    return rawXml;
  }
}
