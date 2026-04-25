package io.minio.reactive;

/** `RemoveObjectArgs`：对齐 minio-java 的对象请求参数对象。 */
public final class RemoveObjectArgs extends ObjectArgs {
  private RemoveObjectArgs(Builder builder) {
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

    public RemoveObjectArgs build() {
      return new RemoveObjectArgs(this);
    }
  }
}
