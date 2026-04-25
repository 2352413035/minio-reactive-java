package io.minio.reactive;

/** `RemoveBucketArgs`：对齐 minio-java 的 bucket 请求参数对象。 */
public final class RemoveBucketArgs extends BucketArgs {
  private RemoveBucketArgs(Builder builder) {
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

    public RemoveBucketArgs build() {
      return new RemoveBucketArgs(this);
    }
  }
}
