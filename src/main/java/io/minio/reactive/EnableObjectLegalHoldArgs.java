package io.minio.reactive;

/** 启用对象 Legal Hold的参数对象。 */
public final class EnableObjectLegalHoldArgs extends ObjectVersionArgs {
  private EnableObjectLegalHoldArgs(Builder builder) {
    super(builder);
  }

  public static Builder builder() {
    return new Builder();
  }

  public static final class Builder extends ObjectVersionArgs.AbstractBuilder<Builder> {
    @Override
    protected Builder self() {
      return this;
    }

    public EnableObjectLegalHoldArgs build() {
      return new EnableObjectLegalHoldArgs(this);
    }
  }
}
