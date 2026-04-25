package io.minio.reactive.credentials;

import io.minio.reactive.ReactiveMinioStsClient;
import io.minio.reactive.messages.sts.AssumeRoleRequest;
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

  /** 使用 ReactiveMinioStsClient 申请 AssumeRole 临时凭证。 */
  public static AssumeRoleProvider fromStsClient(
      ReactiveMinioStsClient client, AssumeRoleRequest request) {
    ReactiveMinioStsClient safeClient = requireValue("client", client);
    AssumeRoleRequest safeRequest = request == null ? AssumeRoleRequest.builder().build() : request;
    return new AssumeRoleProvider(
        fromAssumeRoleResult(safeClient.assumeRoleCredentials(safeRequest)));
  }

  /** 使用默认 AssumeRole 请求申请临时凭证。 */
  public static AssumeRoleProvider fromStsClient(ReactiveMinioStsClient client) {
    return fromStsClient(client, null);
  }
}
