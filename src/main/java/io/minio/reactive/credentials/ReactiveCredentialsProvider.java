package io.minio.reactive.credentials;

import reactor.core.publisher.Mono;

/**
 * 鍝嶅簲寮忓嚟璇佹彁渚涜€呫€? *
 * <p>鍜?minio-java 涓悓姝ョ殑 Provider.fetch() 涓嶅悓锛岃繖閲岀洿鎺ユ妸寮傛鑳藉姏鏀捐繘鎶借薄鏈韩锛屾柟渚垮悗缁鎺ュ閮? * 鍑瘉鏈嶅姟銆? */
@FunctionalInterface
public interface ReactiveCredentialsProvider {
  Mono<ReactiveCredentials> getCredentials();

  static ReactiveCredentialsProvider anonymous() {
    return Mono::empty;
  }
}
