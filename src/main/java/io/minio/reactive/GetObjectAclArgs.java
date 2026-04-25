package io.minio.reactive;

/** `GetObjectAclArgs`：对齐 minio-java 的对象请求参数对象。 */
public final class GetObjectAclArgs extends ObjectArgs {
  private GetObjectAclArgs(Builder builder) {
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

    public GetObjectAclArgs build() {
      return new GetObjectAclArgs(this);
    }
  }
}
