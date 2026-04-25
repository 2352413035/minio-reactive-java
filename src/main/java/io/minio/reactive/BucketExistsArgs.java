package io.minio.reactive;

/** `BucketExistsArgs`：对齐 minio-java 的 bucket 请求参数对象。 */
public final class BucketExistsArgs extends BucketArgs {
  private BucketExistsArgs(Builder builder) {
    super(builder);
  }

  public static Builder builder() {
    return new Builder();
  }

  public static final class Builder extends BucketArgs.AbstractBuilder<Builder> {
    @Override
    protected Builder self() {
      return this;
    }

    public BucketExistsArgs build() {
      return new BucketExistsArgs(this);
    }
  }
}
