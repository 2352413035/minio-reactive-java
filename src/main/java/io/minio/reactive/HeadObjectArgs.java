package io.minio.reactive;

/** `HeadObjectArgs`：对齐 minio-java 的对象请求参数对象。 */
public final class HeadObjectArgs extends HeadObjectBaseArgs {
  private HeadObjectArgs(Builder builder) {
    super(builder);
  }

  public static Builder builder() {
    return new Builder();
  }

  public static final class Builder extends ObjectArgs.AbstractBuilder<Builder> {
    @Override
    protected Builder self() {
      return this;
    }

    public HeadObjectArgs build() {
      return new HeadObjectArgs(this);
    }
  }
}
