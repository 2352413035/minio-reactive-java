package io.minio.reactive.credentials;

import io.minio.reactive.ReactiveMinioStsClient;
import io.minio.reactive.messages.sts.AssumeRoleWithClientGrantsRequest;
import java.util.function.Supplier;
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

  /** 使用 ReactiveMinioStsClient 和 JWT supplier 换取 ClientGrants 临时凭证。 */
  public static ClientGrantsProvider fromStsClient(
      ReactiveMinioStsClient client, Supplier<Jwt> supplier) {
    ReactiveMinioStsClient safeClient = requireValue("client", client);
    Supplier<Jwt> safeSupplier = requireValue("supplier", supplier);
    return new ClientGrantsProvider(
        fromAssumeRoleResult(
            Mono.fromSupplier(safeSupplier)
                .map(jwt -> AssumeRoleWithClientGrantsRequest.of(requireValue("jwt", jwt).token()))
                .flatMap(safeClient::assumeRoleWithClientGrantsCredentials)));
  }
}
