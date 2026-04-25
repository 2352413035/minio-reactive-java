package io.minio.reactive.credentials;

import reactor.core.publisher.Mono;

/**
 * STS 身份类 provider 的响应式基类。
 *
 * <p>当前响应式 SDK 已有 `ReactiveMinioStsClient` 负责真正的 STS HTTP 调用；这些同名 provider
 * 是迁移层边界，用来缓存/桥接 STS 结果，而不是在 provider 内部重复实现一套阻塞 HTTP 客户端。
 */
public abstract class BaseIdentityProvider implements Provider {
  public static final int DEFAULT_DURATION_SECONDS = 3600;
  private final Mono<Credentials> credentials;
  private Credentials cached;

  protected BaseIdentityProvider(Mono<Credentials> credentials) {
    if (credentials == null) {
      throw new IllegalArgumentException("credentials Mono 不能为空");
    }
    this.credentials = credentials.cache();
  }

  protected BaseIdentityProvider(ReactiveCredentialsProvider provider) {
    this(provider == null ? null : provider.getCredentials().map(Credentials::fromReactive));
  }

  @Override
  public synchronized Credentials fetch() {
    if (cached != null && !cached.isExpired()) {
      return cached;
    }
    cached = credentials.block();
    if (cached == null) {
      throw new java.security.ProviderException("STS provider 未返回凭证");
    }
    return cached;
  }
}
