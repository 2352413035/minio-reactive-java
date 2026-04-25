package io.minio.reactive.credentials;

/** 与 minio-java 同名的静态凭证 provider。 */
public final class StaticProvider implements Provider {
  private final Credentials credentials;

  public StaticProvider(String accessKey, String secretKey, String sessionToken) {
    this.credentials = new Credentials(accessKey, secretKey, sessionToken, null);
  }

  public StaticProvider(String accessKey, String secretKey) {
    this(accessKey, secretKey, null);
  }

  public StaticProvider(Credentials credentials) {
    if (credentials == null) {
      throw new IllegalArgumentException("credentials 不能为空");
    }
    this.credentials = credentials;
  }

  @Override
  public Credentials fetch() {
    return credentials;
  }
}
