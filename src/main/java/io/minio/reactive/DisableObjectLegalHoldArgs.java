package io.minio.reactive;

/** 关闭对象 Legal Hold的参数对象。 */
public final class DisableObjectLegalHoldArgs extends ObjectVersionArgs {
  private DisableObjectLegalHoldArgs(Builder builder) {
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

    public DisableObjectLegalHoldArgs build() {
      return new DisableObjectLegalHoldArgs(this);
    }
  }
}
