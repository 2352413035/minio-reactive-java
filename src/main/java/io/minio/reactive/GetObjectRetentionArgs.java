package io.minio.reactive;

/** 获取对象保留策略的参数对象。 */
public final class GetObjectRetentionArgs extends ObjectVersionArgs {
  private GetObjectRetentionArgs(Builder builder) {
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

    public GetObjectRetentionArgs build() {
      return new GetObjectRetentionArgs(this);
    }
  }
}
