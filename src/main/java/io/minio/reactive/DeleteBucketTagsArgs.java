package io.minio.reactive;

/** `DeleteBucketTagsArgs`：bucket 子资源请求参数对象。 */
public final class DeleteBucketTagsArgs extends BucketArgs {
  private DeleteBucketTagsArgs(Builder builder) {
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

    public DeleteBucketTagsArgs build() {
      return new DeleteBucketTagsArgs(this);
    }
  }
}
