package io.minio.reactive;

/** 获取对象标签的参数对象。 */
public final class GetObjectTagsArgs extends ObjectVersionArgs {
  private GetObjectTagsArgs(Builder builder) {
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

    public GetObjectTagsArgs build() {
      return new GetObjectTagsArgs(this);
    }
  }
}
