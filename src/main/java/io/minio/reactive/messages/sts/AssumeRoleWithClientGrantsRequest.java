package io.minio.reactive.messages.sts;

/** 使用 ClientGrants token 申请临时凭证的请求对象。 */
public final class AssumeRoleWithClientGrantsRequest {
  private final String token;

  private AssumeRoleWithClientGrantsRequest(String token) {
    if (token == null || token.trim().isEmpty()) {
      throw new IllegalArgumentException("token 不能为空");
    }
    this.token = token;
  }

  public static AssumeRoleWithClientGrantsRequest of(String token) {
    return new AssumeRoleWithClientGrantsRequest(token);
  }

  public String token() {
    return token;
  }
}
