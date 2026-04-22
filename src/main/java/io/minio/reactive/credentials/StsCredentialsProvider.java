package io.minio.reactive.credentials;

import io.minio.reactive.messages.sts.AssumeRoleResult;
import reactor.core.publisher.Mono;

/**
 * 基于 STS 结果的响应式凭证提供者。
 *
 * <p>调用方可以把 `ReactiveMinioStsClient` 返回的 `AssumeRoleResult` 接入普通对象存储客户端，
 * 从而使用临时 accessKey、secretKey 和 sessionToken 访问 MinIO。
 */
public final class StsCredentialsProvider implements ReactiveCredentialsProvider {
  private final Mono<ReactiveCredentials> credentials;

  private StsCredentialsProvider(Mono<ReactiveCredentials> credentials) {
    this.credentials = credentials.cache();
  }

  public static StsCredentialsProvider from(Mono<AssumeRoleResult> result) {
    return new StsCredentialsProvider(result.map(AssumeRoleResult::credentials));
  }

  public static StsCredentialsProvider fromCredentials(Mono<ReactiveCredentials> credentials) {
    return new StsCredentialsProvider(credentials);
  }

  @Override
  public Mono<ReactiveCredentials> getCredentials() {
    return credentials;
  }
}
