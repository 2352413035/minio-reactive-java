package io.minio.reactive;

/** `GetBucketLocationArgs`：对齐 minio-java 的 bucket 请求参数对象。 */
public final class GetBucketLocationArgs extends BucketArgs {
  private GetBucketLocationArgs(Builder builder) {
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

    public GetBucketLocationArgs build() {
      return new GetBucketLocationArgs(this);
    }
  }
}
