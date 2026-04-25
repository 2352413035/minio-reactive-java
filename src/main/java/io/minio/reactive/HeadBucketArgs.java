package io.minio.reactive;

/** `HeadBucketArgs`：对齐 minio-java 的请求参数对象。 */
public final class HeadBucketArgs extends HeadBucketBaseArgs {
  private HeadBucketArgs(Builder builder) {
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

    public HeadBucketArgs build() {
      return new HeadBucketArgs(this);
    }
  }
}
