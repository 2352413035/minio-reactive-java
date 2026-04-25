package io.minio.reactive.credentials;

import reactor.core.publisher.Mono;

/**
 * 响应式凭证提供者接口。
 *
 * <p>和传统同步 SDK 中“直接返回凭证”的接口不同，这里返回 {@link Mono}，
 * 目的是把“拿凭证”这件事本身也纳入响应式链路。
 * 这样后续接入远程凭证服务、刷新临时凭证时，不需要再引入阻塞调用。
 */
@FunctionalInterface
public interface ReactiveCredentialsProvider {
  Mono<ReactiveCredentials> getCredentials();

  static ReactiveCredentialsProvider anonymous() {
    return Mono::empty;
  }

  /** 把 minio-java 风格 Provider 桥接到响应式客户端使用的 provider。 */
  static ReactiveCredentialsProvider from(Provider provider) {
    if (provider == null) {
      throw new IllegalArgumentException("provider 不能为空");
    }
    return provider.asReactiveProvider();
  }
}
