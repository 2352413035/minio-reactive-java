package io.minio.reactive.credentials;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.beans.ConstructorProperties;

/** WebIdentity / ClientGrants 场景使用的 JWT 包装对象。 */
public final class Jwt {
  @JsonProperty("access_token")
  private final String token;

  @JsonProperty("expires_in")
  private final int expiry;

  @JsonCreator
  @ConstructorProperties({"access_token", "expires_in"})
  public Jwt(@JsonProperty("access_token") String token, @JsonProperty("expires_in") int expiry) {
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
