package io.minio.reactive;

/** `DeleteBucketPolicyArgs`：bucket 子资源请求参数对象。 */
public final class DeleteBucketPolicyArgs extends BucketArgs {
  private DeleteBucketPolicyArgs(Builder builder) {
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

    public DeleteBucketPolicyArgs build() {
      return new DeleteBucketPolicyArgs(this);
    }
  }
}
