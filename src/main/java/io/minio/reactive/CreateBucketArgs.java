package io.minio.reactive;

/** `CreateBucketArgs`：对齐 minio-java 的请求参数对象。 */
public final class CreateBucketArgs extends CreateBucketBaseArgs {
  private CreateBucketArgs(Builder builder) {
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

    public CreateBucketArgs build() {
      return new CreateBucketArgs(this);
    }
  }
}
