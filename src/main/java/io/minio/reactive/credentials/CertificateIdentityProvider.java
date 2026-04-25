package io.minio.reactive.credentials;

import io.minio.reactive.ReactiveMinioStsClient;
import io.minio.reactive.messages.sts.AssumeRoleWithCertificateRequest;
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

  /** 使用已配置证书的 ReactiveMinioStsClient 换取证书身份临时凭证。 */
  public static CertificateIdentityProvider fromStsClient(
      ReactiveMinioStsClient client, AssumeRoleWithCertificateRequest request) {
    ReactiveMinioStsClient safeClient = requireValue("client", client);
    AssumeRoleWithCertificateRequest safeRequest =
        request == null ? AssumeRoleWithCertificateRequest.create() : request;
    return new CertificateIdentityProvider(
        fromAssumeRoleResult(safeClient.assumeRoleWithCertificateCredentials(safeRequest)));
  }

  /** 使用默认证书身份请求换取临时凭证。 */
  public static CertificateIdentityProvider fromStsClient(ReactiveMinioStsClient client) {
    return fromStsClient(client, null);
  }
}
