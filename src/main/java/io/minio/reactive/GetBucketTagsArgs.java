package io.minio.reactive;

/** `GetBucketTagsArgs`：bucket 子资源请求参数对象。 */
public final class GetBucketTagsArgs extends BucketArgs {
  private GetBucketTagsArgs(Builder builder) {
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

    public GetBucketTagsArgs build() {
      return new GetBucketTagsArgs(this);
    }
  }
}
