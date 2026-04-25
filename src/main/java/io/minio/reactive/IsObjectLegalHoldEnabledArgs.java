package io.minio.reactive;

/** 判断对象 Legal Hold的参数对象。 */
public final class IsObjectLegalHoldEnabledArgs extends ObjectVersionArgs {
  private IsObjectLegalHoldEnabledArgs(Builder builder) {
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

    public IsObjectLegalHoldEnabledArgs build() {
      return new IsObjectLegalHoldEnabledArgs(this);
    }
  }
}
