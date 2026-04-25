package io.minio.reactive.credentials;

import reactor.core.publisher.Mono;

/** Web identity provider 的响应式迁移边界。 */
public final class WebIdentityProvider extends WebIdentityClientGrantsProvider {
  public WebIdentityProvider(ReactiveCredentialsProvider provider) {
    super(provider);
  }

  public WebIdentityProvider(Mono<Credentials> credentials) {
    super(credentials);
  }

  public static WebIdentityProvider fromReactive(Mono<ReactiveCredentials> credentials) {
    return new WebIdentityProvider(credentials.map(Credentials::fromReactive));
  }
}
