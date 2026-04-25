package io.minio.reactive;

/** `DeleteObjectLockConfigurationArgs`：bucket 子资源请求参数对象。 */
public final class DeleteObjectLockConfigurationArgs extends BucketArgs {
  private DeleteObjectLockConfigurationArgs(Builder builder) {
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

    public DeleteObjectLockConfigurationArgs build() {
      return new DeleteObjectLockConfigurationArgs(this);
    }
  }
}
