package io.minio.reactive.credentials;

/**
 * 响应式 SDK 使用的凭证对象。
 *
 * <p>当前只包含三项最基础的数据：
 *
 * <ul>
 *   <li>accessKey
 *   <li>secretKey
 *   <li>sessionToken
 * </ul>
 *
 * <p>之所以保留 sessionToken，是为了后续支持临时凭证场景，例如 STS、AssumeRole、WebIdentity。
 */
public final class ReactiveCredentials {
  private final String accessKey;
  private final String secretKey;
  private final String sessionToken;

  private ReactiveCredentials(String accessKey, String secretKey, String sessionToken) {
    this.accessKey = accessKey;
    this.secretKey = secretKey;
    this.sessionToken = sessionToken;
  }

  public static ReactiveCredentials of(String accessKey, String secretKey) {
    return new ReactiveCredentials(accessKey, secretKey, null);
  }

  public static ReactiveCredentials of(
      String accessKey, String secretKey, String sessionToken) {
    return new ReactiveCredentials(accessKey, secretKey, sessionToken);
  }

  public static ReactiveCredentials anonymous() {
    return new ReactiveCredentials(null, null, null);
  }

  public String accessKey() {
    return accessKey;
  }

  public String secretKey() {
    return secretKey;
  }

  public String sessionToken() {
    return sessionToken;
  }

  public boolean isAnonymous() {
    return accessKey == null || secretKey == null;
  }
}
