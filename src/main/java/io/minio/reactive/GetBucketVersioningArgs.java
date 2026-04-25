package io.minio.reactive;

/** `GetBucketVersioningArgs`：bucket 子资源请求参数对象。 */
public final class GetBucketVersioningArgs extends BucketArgs {
  private GetBucketVersioningArgs(Builder builder) {
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

    public GetBucketVersioningArgs build() {
      return new GetBucketVersioningArgs(this);
    }
  }
}
