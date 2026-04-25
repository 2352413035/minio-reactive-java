package io.minio.reactive.credentials;

/** WebIdentity / ClientGrants 场景使用的 JWT 包装对象。 */
public final class Jwt {
  private final String token;
  private final int expiry;

  public Jwt(String token, int expiry) {
    this.token = CredentialProviderSupport.requireText("token", token);
    this.expiry = expiry;
  }

  public String token() {
    return token;
  }

  public int expiry() {
    return expiry;
  }
}
