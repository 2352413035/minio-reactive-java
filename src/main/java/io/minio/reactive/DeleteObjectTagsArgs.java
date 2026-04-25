package io.minio.reactive;

/** `DeleteObjectTagsArgs`：对齐 minio-java 的对象请求参数对象。 */
public final class DeleteObjectTagsArgs extends ObjectArgs {
  private DeleteObjectTagsArgs(Builder builder) {
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

    public DeleteObjectTagsArgs build() {
      return new DeleteObjectTagsArgs(this);
    }
  }
}
