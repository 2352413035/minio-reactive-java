package io.minio.reactive;

import io.minio.reactive.messages.BucketVersioningConfiguration;

/** 设置 bucket versioning 的参数对象，可传 XML 或强类型配置。 */
public final class SetBucketVersioningArgs extends BucketArgs {
  private final String versioningXml;
  private final BucketVersioningConfiguration configuration;

  private SetBucketVersioningArgs(Builder builder) {
    super(builder);
    this.versioningXml = builder.versioningXml;
    this.configuration = builder.configuration;
    if ((versioningXml == null || versioningXml.trim().isEmpty()) && configuration == null) {
      throw new IllegalArgumentException("versioningXml 或 configuration 必须提供一个");
    }
  }

  public String versioningXml() {
    return versioningXml;
  }

  public BucketVersioningConfiguration configuration() {
    return configuration;
  }

  public static Builder builder() {
    return new Builder();
  }

  public static final class Builder extends BucketArgs.AbstractBuilder<Builder> {
    private String versioningXml;
    private BucketVersioningConfiguration configuration;

    public Builder versioningXml(String versioningXml) {
      this.versioningXml = versioningXml;
      return this;
    }

    public Builder configuration(BucketVersioningConfiguration configuration) {
      this.configuration = configuration;
      return this;
    }

    @Override
    protected Builder self() {
      return this;
    }

    public SetBucketVersioningArgs build() {
      return new SetBucketVersioningArgs(this);
    }
  }
}
