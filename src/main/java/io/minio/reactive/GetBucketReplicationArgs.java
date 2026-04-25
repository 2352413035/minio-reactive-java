package io.minio.reactive;

/** `GetBucketReplicationArgs`：bucket 子资源请求参数对象。 */
public final class GetBucketReplicationArgs extends BucketArgs {
  private GetBucketReplicationArgs(Builder builder) {
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

    public GetBucketReplicationArgs build() {
      return new GetBucketReplicationArgs(this);
    }
  }
}
