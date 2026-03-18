package io.minio.reactive.credentials;

/**
 * 閸濆秴绨插?SDK 娴ｈ法鏁ら惃鍕殶鐠囦焦膩閸ㄥ鈧? *
 * <p>閸忓牅绻氶幐浣虹暆閸楁洩绱濋崣顏冪箽閻ｆ瑩娼ら幀浣稿殶鐠囦礁鎷伴崠鍨倳鐠佸潡妫堕棁鈧憰浣烘畱鐎涙顔岄妴鍌氭倵缂侇厼顩ч弸婊勫复閸?STS閵嗕竸ssumeRole閵嗕箘ebIdentity閿? * 閸愬秷藟閸?expiry 缁涘鐫橀幀褋鈧? */
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
