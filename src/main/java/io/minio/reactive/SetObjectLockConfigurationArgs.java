package io.minio.reactive;

/** 设置 object-lock XML 的参数对象。 */
public final class SetObjectLockConfigurationArgs extends BucketArgs {
  private final String objectLockXml;

  private SetObjectLockConfigurationArgs(Builder builder) {
    super(builder);
    this.objectLockXml = requireText("objectLockXml", builder.objectLockXml);
  }

  public String objectLockXml() {
    return objectLockXml;
  }

  public static Builder builder() {
    return new Builder();
  }

  public static final class Builder extends BucketArgs.AbstractBuilder<Builder> {
    private String objectLockXml;

    public Builder objectLockXml(String objectLockXml) {
      this.objectLockXml = objectLockXml;
      return this;
    }

    @Override
    protected Builder self() {
      return this;
    }

    public SetObjectLockConfigurationArgs build() {
      return new SetObjectLockConfigurationArgs(this);
    }
  }
}
