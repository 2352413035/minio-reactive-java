package io.minio.reactive.credentials;

import reactor.core.publisher.Mono;

/** AssumeRole provider 的响应式迁移边界。 */
public final class AssumeRoleProvider extends BaseIdentityProvider {
  public AssumeRoleProvider(ReactiveCredentialsProvider provider) {
    super(provider);
  }

  public AssumeRoleProvider(Mono<Credentials> credentials) {
    super(credentials);
  }

  public static AssumeRoleProvider fromReactive(Mono<ReactiveCredentials> credentials) {
    return new AssumeRoleProvider(credentials.map(Credentials::fromReactive));
  }
}
