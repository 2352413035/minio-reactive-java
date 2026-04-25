package io.minio.reactive;

import io.minio.reactive.messages.BucketNotificationConfiguration;

/** 设置 bucket notification 的参数对象，可传 XML 或强类型配置。 */
public final class SetBucketNotificationArgs extends BucketArgs {
  private final String notificationXml;
  private final BucketNotificationConfiguration configuration;

  private SetBucketNotificationArgs(Builder builder) {
    super(builder);
    this.notificationXml = builder.notificationXml;
    this.configuration = builder.configuration;
    if ((notificationXml == null || notificationXml.trim().isEmpty()) && configuration == null) {
      throw new IllegalArgumentException("notificationXml 或 configuration 必须提供一个");
    }
  }

  public String notificationXml() {
    return notificationXml;
  }

  public BucketNotificationConfiguration configuration() {
    return configuration;
  }

  public static Builder builder() {
    return new Builder();
  }

  public static final class Builder extends BucketArgs.AbstractBuilder<Builder> {
    private String notificationXml;
    private BucketNotificationConfiguration configuration;

    public Builder notificationXml(String notificationXml) {
      this.notificationXml = notificationXml;
      return this;
    }

    public Builder configuration(BucketNotificationConfiguration configuration) {
      this.configuration = configuration;
      return this;
    }

    @Override
    protected Builder self() {
      return this;
    }

    public SetBucketNotificationArgs build() {
      return new SetBucketNotificationArgs(this);
    }
  }
}
