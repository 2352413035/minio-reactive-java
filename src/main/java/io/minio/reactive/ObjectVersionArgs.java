package io.minio.reactive;

/** 带 versionId 的对象参数基类。 */
public abstract class ObjectVersionArgs extends ObjectArgs {
  private final String versionId;

  protected ObjectVersionArgs(AbstractBuilder<?> builder) {
    super(builder);
    this.versionId = builder.versionId;
  }

  public String versionId() {
    return versionId;
  }

  public abstract static class AbstractBuilder<B extends AbstractBuilder<B>>
      extends ObjectArgs.AbstractBuilder<B> {
    private String versionId;

    public B versionId(String versionId) {
      this.versionId = versionId;
      return self();
    }
  }
}
