package io.minio.reactive.credentials;

import java.nio.file.Path;
import java.nio.file.Paths;

/** 读取 AWS credentials INI 文件的 provider。 */
public final class AwsConfigProvider extends EnvironmentProvider {
  private final String filename;
  private final String profile;

  public AwsConfigProvider(String filename, String profile) {
    if (filename != null && filename.trim().isEmpty()) {
      throw new IllegalArgumentException("filename 不能为空字符串");
    }
    if (profile != null && profile.trim().isEmpty()) {
      throw new IllegalArgumentException("profile 不能为空字符串");
    }
    this.filename = filename;
    this.profile = profile;
  }

  public AwsConfigProvider() {
    this(null, null);
  }

  @Override
  public Credentials fetch() {
    String actualFilename = filename != null ? filename : getProperty("AWS_SHARED_CREDENTIALS_FILE");
    if (actualFilename == null) {
      actualFilename = Paths.get(System.getProperty("user.home"), ".aws", "credentials").toString();
    }
    String actualProfile = profile != null ? profile : getProperty("AWS_PROFILE");
    if (actualProfile == null) {
      actualProfile = "default";
    }
    Path path = Paths.get(actualFilename);
    return CredentialProviderSupport.readAwsProfile(path, actualProfile);
  }
}
