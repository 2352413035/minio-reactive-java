package io.minio.reactive;

/** 设置 bucket lifecycle XML 的参数对象。 */
public final class SetBucketLifecycleArgs extends BucketArgs {
  private final String lifecycleXml;

  private SetBucketLifecycleArgs(Builder builder) {
    super(builder);
    this.lifecycleXml = requireText("lifecycleXml", builder.lifecycleXml);
  }

  public String lifecycleXml() {
    return lifecycleXml;
  }

  public static Builder builder() {
    return new Builder();
  }

  public static final class Builder extends BucketArgs.AbstractBuilder<Builder> {
    private String lifecycleXml;

    public Builder lifecycleXml(String lifecycleXml) {
      this.lifecycleXml = lifecycleXml;
      return this;
    }

    @Override
    protected Builder self() {
      return this;
    }

    public SetBucketLifecycleArgs build() {
      return new SetBucketLifecycleArgs(this);
    }
  }
}
