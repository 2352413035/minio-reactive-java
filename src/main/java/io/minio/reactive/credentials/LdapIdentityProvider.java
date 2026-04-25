package io.minio.reactive.credentials;

import io.minio.reactive.ReactiveMinioStsClient;
import io.minio.reactive.messages.sts.AssumeRoleWithLdapIdentityRequest;
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

  /** 使用 ReactiveMinioStsClient 通过 LDAP 身份换取临时凭证。 */
  public static LdapIdentityProvider fromStsClient(
      ReactiveMinioStsClient client, String username, String password) {
    ReactiveMinioStsClient safeClient = requireValue("client", client);
    return new LdapIdentityProvider(
        fromAssumeRoleResult(
            safeClient.assumeRoleWithLdapCredentials(
                AssumeRoleWithLdapIdentityRequest.of(username, password))));
  }
}
