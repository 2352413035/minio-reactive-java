package io.minio.reactive;

/** `GetBucketEncryptionArgs`：bucket 子资源请求参数对象。 */
public final class GetBucketEncryptionArgs extends BucketArgs {
  private GetBucketEncryptionArgs(Builder builder) {
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

    public GetBucketEncryptionArgs build() {
      return new GetBucketEncryptionArgs(this);
    }
  }
}
