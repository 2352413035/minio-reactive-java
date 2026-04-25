package io.minio.reactive;

/** 带 bucket/object 名称的 Args 基类，供具体对象请求对象继承。 */
public abstract class ObjectArgs extends BucketArgs {
  private final String object;

  protected ObjectArgs(AbstractBuilder<?> builder) {
    super(builder);
    this.object = requireText("object", builder.object);
  }

  public String object() {
    return object;
  }

  public abstract static class AbstractBuilder<B extends AbstractBuilder<B>>
      extends BucketArgs.AbstractBuilder<B> {
    private String object;

    public B object(String object) {
      this.object = object;
      return self();
    }
  }
}
