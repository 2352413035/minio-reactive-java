package io.minio.reactive;

import io.minio.reactive.messages.BucketCorsConfiguration;

/** 设置 bucket CORS 的参数对象。 */
public final class SetBucketCorsArgs extends BucketArgs {
  private final BucketCorsConfiguration configuration;

  private SetBucketCorsArgs(Builder builder) {
    super(builder);
    if (builder.configuration == null) {
      throw new IllegalArgumentException("configuration 不能为空");
    }
    this.configuration = builder.configuration;
  }

  public BucketCorsConfiguration configuration() {
    return configuration;
  }

  public static Builder builder() {
    return new Builder();
  }

  public static final class Builder extends BucketArgs.AbstractBuilder<Builder> {
    private BucketCorsConfiguration configuration;

    public Builder configuration(BucketCorsConfiguration configuration) {
      this.configuration = configuration;
      return this;
    }

    @Override
    protected Builder self() {
      return this;
    }

    public SetBucketCorsArgs build() {
      return new SetBucketCorsArgs(this);
    }
  }
}
