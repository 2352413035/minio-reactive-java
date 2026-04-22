package io.minio.reactive.messages.sts;

/** 使用 WebIdentity token 申请临时凭证的请求对象。 */
public final class AssumeRoleWithWebIdentityRequest {
  private final String webIdentityToken;

  private AssumeRoleWithWebIdentityRequest(String webIdentityToken) {
    if (webIdentityToken == null || webIdentityToken.trim().isEmpty()) {
      throw new IllegalArgumentException("webIdentityToken 不能为空");
    }
    this.webIdentityToken = webIdentityToken;
  }

  public static AssumeRoleWithWebIdentityRequest of(String webIdentityToken) {
    return new AssumeRoleWithWebIdentityRequest(webIdentityToken);
  }

  public String webIdentityToken() {
    return webIdentityToken;
  }
}
