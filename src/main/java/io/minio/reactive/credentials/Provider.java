package io.minio.reactive.credentials;

import reactor.core.publisher.Mono;

/**
 * 与 minio-java 同名的同步凭证提供者接口。
 *
 * <p>响应式 SDK 内部仍优先使用 {@link ReactiveCredentialsProvider}。这个接口用于迁移兼容，
 * 通过 {@link #asReactiveProvider()} 可以安全桥接到响应式客户端 builder。
 */
public interface Provider {
  Credentials fetch();

  default ReactiveCredentialsProvider asReactiveProvider() {
    return () -> Mono.fromCallable(() -> fetch().toReactiveCredentials());
  }
}
