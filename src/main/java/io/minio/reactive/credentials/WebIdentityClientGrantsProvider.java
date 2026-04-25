package io.minio.reactive.credentials;

import reactor.core.publisher.Mono;

/** WebIdentity 与 ClientGrants provider 的共同迁移基类。 */
public abstract class WebIdentityClientGrantsProvider extends BaseIdentityProvider {
  public static final int MIN_DURATION_SECONDS = 900;
  public static final int MAX_DURATION_SECONDS = 604800;

  protected WebIdentityClientGrantsProvider(ReactiveCredentialsProvider provider) {
    super(provider);
  }

  protected WebIdentityClientGrantsProvider(Mono<Credentials> credentials) {
    super(credentials);
  }

  protected int getDurationSeconds(int expiry) {
    if (expiry > MAX_DURATION_SECONDS) {
      return MAX_DURATION_SECONDS;
    }
    if (expiry <= 0) {
      return expiry;
    }
    return expiry < MIN_DURATION_SECONDS ? MIN_DURATION_SECONDS : expiry;
  }
}
