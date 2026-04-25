package io.minio.reactive.credentials;

import reactor.core.publisher.Mono;

/** LDAP identity provider 的响应式迁移边界。 */
public final class LdapIdentityProvider extends BaseIdentityProvider {
  public LdapIdentityProvider(ReactiveCredentialsProvider provider) {
    super(provider);
  }

  public LdapIdentityProvider(Mono<Credentials> credentials) {
    super(credentials);
  }

  public static LdapIdentityProvider fromReactive(Mono<ReactiveCredentials> credentials) {
    return new LdapIdentityProvider(credentials.map(Credentials::fromReactive));
  }
}
