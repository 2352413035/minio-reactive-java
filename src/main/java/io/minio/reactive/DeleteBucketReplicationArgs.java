package io.minio.reactive;

/** `DeleteBucketReplicationArgs`：bucket 子资源请求参数对象。 */
public final class DeleteBucketReplicationArgs extends BucketArgs {
  private DeleteBucketReplicationArgs(Builder builder) {
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

    public DeleteBucketReplicationArgs build() {
      return new DeleteBucketReplicationArgs(this);
    }
  }
}
