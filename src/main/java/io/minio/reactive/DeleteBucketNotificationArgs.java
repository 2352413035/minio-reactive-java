package io.minio.reactive;

/** `DeleteBucketNotificationArgs`：bucket 子资源请求参数对象。 */
public final class DeleteBucketNotificationArgs extends BucketArgs {
  private DeleteBucketNotificationArgs(Builder builder) {
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

    public DeleteBucketNotificationArgs build() {
      return new DeleteBucketNotificationArgs(this);
    }
  }
}
