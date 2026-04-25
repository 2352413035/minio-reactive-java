package io.minio.reactive.credentials;

import io.minio.reactive.ReactiveMinioStsClient;
import io.minio.reactive.messages.sts.AssumeRoleWithWebIdentityRequest;
import java.util.function.Supplier;
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

  /** 使用 ReactiveMinioStsClient 和 JWT supplier 换取 WebIdentity 临时凭证。 */
  public static WebIdentityProvider fromStsClient(
      ReactiveMinioStsClient client, Supplier<Jwt> supplier) {
    ReactiveMinioStsClient safeClient = requireValue("client", client);
    Supplier<Jwt> safeSupplier = requireValue("supplier", supplier);
    return new WebIdentityProvider(
        fromAssumeRoleResult(
            Mono.fromSupplier(safeSupplier)
                .map(jwt -> AssumeRoleWithWebIdentityRequest.of(requireValue("jwt", jwt).token()))
                .flatMap(safeClient::assumeRoleWithWebIdentityCredentials)));
  }
}
