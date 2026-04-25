package io.minio.reactive.credentials;

import java.nio.file.Path;
import java.nio.file.Paths;

/** 读取 mc / MinIO 客户端 config.json 的 provider。 */
public final class MinioClientConfigProvider extends EnvironmentProvider {
  private final String filename;
  private final String alias;

  public MinioClientConfigProvider(String filename, String alias) {
    if (filename != null && filename.trim().isEmpty()) {
      throw new IllegalArgumentException("filename 不能为空字符串");
    }
    if (alias != null && alias.trim().isEmpty()) {
      throw new IllegalArgumentException("alias 不能为空字符串");
    }
    this.filename = filename;
    this.alias = alias;
  }

  public MinioClientConfigProvider() {
    this(null, null);
  }

  @Override
  public Credentials fetch() {
    String actualFilename = filename != null ? filename : getProperty("MINIO_SHARED_CREDENTIALS_FILE");
    Path path = actualFilename == null ? CredentialProviderSupport.defaultMinioConfigPath() : Paths.get(actualFilename);
    String actualAlias = alias != null ? alias : getProperty("MINIO_ALIAS");
    if (actualAlias == null) {
      actualAlias = "s3";
    }
    return CredentialProviderSupport.readMinioConfig(path, actualAlias);
  }
}
