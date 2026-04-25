package io.minio.reactive;

/** `DeleteBucketCorsArgs`：bucket 子资源请求参数对象。 */
public final class DeleteBucketCorsArgs extends BucketArgs {
  private DeleteBucketCorsArgs(Builder builder) {
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

    public DeleteBucketCorsArgs build() {
      return new DeleteBucketCorsArgs(this);
    }
  }
}
