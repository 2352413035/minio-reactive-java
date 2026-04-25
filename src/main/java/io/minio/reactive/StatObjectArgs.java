package io.minio.reactive;

/** `StatObjectArgs`：对齐 minio-java 的对象请求参数对象。 */
public final class StatObjectArgs extends ObjectArgs {
  private StatObjectArgs(Builder builder) {
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

    public StatObjectArgs build() {
      return new StatObjectArgs(this);
    }
  }
}
