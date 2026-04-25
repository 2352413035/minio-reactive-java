package io.minio.reactive.credentials;

import java.time.Duration;
import java.time.ZonedDateTime;

/**
 * 与 minio-java 同名的凭证对象。
 *
 * <p>它用于迁移层和 provider 体系；真正发起请求时会转换为 {@link ReactiveCredentials}。
 * `toString()` 会脱敏，避免日志或异常中泄露 secret/sessionToken。
 */
public final class Credentials {
  private final String accessKey;
  private final String secretKey;
  private final String sessionToken;
  private final ZonedDateTime expiration;

  public Credentials(String accessKey, String secretKey, String sessionToken, ZonedDateTime expiration) {
    this.accessKey = requireText("accessKey", accessKey);
    this.secretKey = requireText("secretKey", secretKey);
    this.sessionToken = sessionToken;
    this.expiration = expiration;
  }

  public Credentials(String accessKey, String secretKey, String sessionToken) {
    this(accessKey, secretKey, sessionToken, null);
  }

  public Credentials(String accessKey, String secretKey) {
    this(accessKey, secretKey, null, null);
  }

  public static Credentials fromReactive(ReactiveCredentials credentials) {
    if (credentials == null || credentials.isAnonymous()) {
      throw new IllegalArgumentException("ReactiveCredentials 不能为空且不能是匿名凭证");
    }
    return new Credentials(credentials.accessKey(), credentials.secretKey(), credentials.sessionToken(), null);
  }

  public ReactiveCredentials toReactiveCredentials() {
    return ReactiveCredentials.of(accessKey, secretKey, sessionToken);
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

  public ZonedDateTime expiration() {
    return expiration;
  }

  public boolean isExpired() {
    return expiration != null && ZonedDateTime.now().plus(Duration.ofSeconds(10)).isAfter(expiration);
  }

  @Override
  public String toString() {
    return "Credentials{accessKey='" + redact(accessKey) + "', secretKey='****', sessionToken="
        + (sessionToken == null ? "无" : "****") + ", expiration=" + expiration + "}";
  }

  private static String requireText(String name, String value) {
    if (value == null || value.trim().isEmpty()) {
      throw new IllegalArgumentException(name + " 不能为空");
    }
    return value;
  }

  private static String redact(String value) {
    if (value == null || value.length() <= 4) {
      return "****";
    }
    return value.substring(0, 2) + "****" + value.substring(value.length() - 2);
  }
}
