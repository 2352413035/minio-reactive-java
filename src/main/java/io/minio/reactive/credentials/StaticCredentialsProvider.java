package io.minio.reactive.credentials;

import java.util.Objects;
import reactor.core.publisher.Mono;

/**
 * 闂堟瑦鈧礁鍤熺拠浣瑰絹娓氭稖鈧懌鈧? */
public final class StaticCredentialsProvider implements ReactiveCredentialsProvider {
  private final ReactiveCredentials credentials;

  public StaticCredentialsProvider(String accessKey, String secretKey) {
    this.credentials =
        ReactiveCredentials.of(
            Objects.requireNonNull(accessKey, "accessKey must not be null"),
            Objects.requireNonNull(secretKey, "secretKey must not be null"));
  }

  @Override
  public Mono<ReactiveCredentials> getCredentials() {
    return Mono.just(credentials);
  }
}
