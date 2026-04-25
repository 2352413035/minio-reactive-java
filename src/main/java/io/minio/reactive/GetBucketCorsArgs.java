package io.minio.reactive;

/** `GetBucketCorsArgs`：bucket 子资源请求参数对象。 */
public final class GetBucketCorsArgs extends BucketArgs {
  private GetBucketCorsArgs(Builder builder) {
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

    public GetBucketCorsArgs build() {
      return new GetBucketCorsArgs(this);
    }
  }
}
