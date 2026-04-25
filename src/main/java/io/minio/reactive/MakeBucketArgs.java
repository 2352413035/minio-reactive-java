package io.minio.reactive;

/** `MakeBucketArgs`：对齐 minio-java 的 bucket 请求参数对象。 */
public final class MakeBucketArgs extends BucketArgs {
  private MakeBucketArgs(Builder builder) {
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

    public MakeBucketArgs build() {
      return new MakeBucketArgs(this);
    }
  }
}
