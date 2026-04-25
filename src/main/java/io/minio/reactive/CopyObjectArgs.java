package io.minio.reactive;

/** 复制对象参数对象，描述目标对象和源对象。 */
public final class CopyObjectArgs extends ObjectArgs {
  private final String sourceBucket;
  private final String sourceObject;

  private CopyObjectArgs(Builder builder) {
    super(builder);
    this.sourceBucket = requireText("sourceBucket", builder.sourceBucket);
    this.sourceObject = requireText("sourceObject", builder.sourceObject);
  }

  public String sourceBucket() {
    return sourceBucket;
  }

  public String sourceObject() {
    return sourceObject;
  }

  public static Builder builder() {
    return new Builder();
  }

  public static final class Builder extends ObjectArgs.AbstractBuilder<Builder> {
    private String sourceBucket;
    private String sourceObject;

    public Builder sourceBucket(String sourceBucket) {
      this.sourceBucket = sourceBucket;
      return this;
    }

    public Builder sourceObject(String sourceObject) {
      this.sourceObject = sourceObject;
      return this;
    }

    @Override
    protected Builder self() {
      return this;
    }

    public CopyObjectArgs build() {
      return new CopyObjectArgs(this);
    }
  }
}
