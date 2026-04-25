package io.minio.reactive.credentials;

import reactor.core.publisher.Mono;

/** Client grants provider 的响应式迁移边界。 */
public final class ClientGrantsProvider extends WebIdentityClientGrantsProvider {
  public ClientGrantsProvider(ReactiveCredentialsProvider provider) {
    super(provider);
  }

  public ClientGrantsProvider(Mono<Credentials> credentials) {
    super(credentials);
  }

  public static ClientGrantsProvider fromReactive(Mono<ReactiveCredentials> credentials) {
    return new ClientGrantsProvider(credentials.map(Credentials::fromReactive));
  }
}
