package io.minio.reactive.credentials;

/** 使用 MINIO_ACCESS_KEY / MINIO_SECRET_KEY 的 provider。 */
public final class MinioEnvironmentProvider extends EnvironmentProvider {
  @Override
  public Credentials fetch() {
    return new Credentials(
        requireProperty("MINIO_ACCESS_KEY", "MinIO accessKey"),
        requireProperty("MINIO_SECRET_KEY", "MinIO secretKey"),
        null,
        null);
  }
}
