package io.minio.reactive;

/** `GetBucketNotificationArgs`：bucket 子资源请求参数对象。 */
public final class GetBucketNotificationArgs extends BucketArgs {
  private GetBucketNotificationArgs(Builder builder) {
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

    public GetBucketNotificationArgs build() {
      return new GetBucketNotificationArgs(this);
    }
  }
}
