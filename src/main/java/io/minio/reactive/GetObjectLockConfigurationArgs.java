package io.minio.reactive;

/** `GetObjectLockConfigurationArgs`：bucket 子资源请求参数对象。 */
public final class GetObjectLockConfigurationArgs extends BucketArgs {
  private GetObjectLockConfigurationArgs(Builder builder) {
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

    public GetObjectLockConfigurationArgs build() {
      return new GetObjectLockConfigurationArgs(this);
    }
  }
}
