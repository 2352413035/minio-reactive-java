package io.minio.reactive.credentials;

import java.util.Objects;
import reactor.core.publisher.Mono;

/**
 * 静态凭证提供者。
 *
 * <p>这是当前项目最简单的凭证实现，适合本地开发和最小示例。
 * 它不做刷新，也不访问外部系统，只是把固定的 accessKey/secretKey 包装成响应式返回值。
 */
public final class StaticCredentialsProvider implements ReactiveCredentialsProvider {
  private final ReactiveCredentials credentials;

  public StaticCredentialsProvider(String accessKey, String secretKey) {
    this(accessKey, secretKey, null);
  }

  public StaticCredentialsProvider(String accessKey, String secretKey, String sessionToken) {
    this.credentials =
        ReactiveCredentials.of(
            Objects.requireNonNull(accessKey, "accessKey 不能为空"),
            Objects.requireNonNull(secretKey, "secretKey 不能为空"),
            sessionToken);
  }

  @Override
  public Mono<ReactiveCredentials> getCredentials() {
    return Mono.just(credentials);
  }
}
