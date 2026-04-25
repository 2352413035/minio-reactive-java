package io.minio.reactive.credentials;

import java.security.ProviderException;
import java.util.Arrays;
import java.util.List;

/** 按顺序尝试多个 provider 的链式凭证提供者。 */
public final class ChainedProvider implements Provider {
  private final List<Provider> providers;
  private Provider currentProvider;
  private Credentials credentials;

  public ChainedProvider(Provider... providers) {
    if (providers == null || providers.length == 0) {
      throw new IllegalArgumentException("providers 不能为空");
    }
    this.providers = Arrays.asList(providers);
  }

  @Override
  public synchronized Credentials fetch() {
    if (credentials != null && !credentials.isExpired()) {
      return credentials;
    }
    if (currentProvider != null) {
      try {
        credentials = currentProvider.fetch();
        return credentials;
      } catch (ProviderException ignored) {
        // 当前 provider 失败后继续按顺序回退，避免一次临时故障锁死链路。
      }
    }
    for (Provider provider : providers) {
      if (provider == null) {
        continue;
      }
      try {
        credentials = provider.fetch();
        currentProvider = provider;
        return credentials;
      } catch (ProviderException ignored) {
        // 和 minio-java 保持一致：单个 provider 失败时继续尝试后续 provider。
      }
    }
    throw new ProviderException("所有 provider 都无法获取凭证");
  }
}
