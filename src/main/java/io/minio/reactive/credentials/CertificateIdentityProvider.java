package io.minio.reactive.credentials;

import reactor.core.publisher.Mono;

/** Certificate identity provider 的响应式迁移边界。 */
public final class CertificateIdentityProvider extends BaseIdentityProvider {
  public CertificateIdentityProvider(ReactiveCredentialsProvider provider) {
    super(provider);
  }

  public CertificateIdentityProvider(Mono<Credentials> credentials) {
    super(credentials);
  }

  public static CertificateIdentityProvider fromReactive(Mono<ReactiveCredentials> credentials) {
    return new CertificateIdentityProvider(credentials.map(Credentials::fromReactive));
  }
}
