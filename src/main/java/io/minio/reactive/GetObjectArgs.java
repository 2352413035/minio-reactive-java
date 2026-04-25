package io.minio.reactive;

/** 读取对象参数对象，可选 offset/length 用于范围读取。 */
public final class GetObjectArgs extends ObjectArgs {
  private final Long offset;
  private final Long length;

  private GetObjectArgs(Builder builder) {
    super(builder);
    this.offset = builder.offset;
    this.length = builder.length;
    if (this.offset != null && this.length == null) {
      throw new IllegalArgumentException("使用 offset 时必须同时提供 length");
    }
  }

  public Long offset() {
    return offset;
  }

  public Long length() {
    return length;
  }

  public static Builder builder() {
    return new Builder();
  }

  public static final class Builder extends ObjectArgs.AbstractBuilder<Builder> {
    private Long offset;
    private Long length;

    public Builder offset(long offset) {
      this.offset = requireNonNegative("offset", offset);
      return this;
    }

    public Builder length(long length) {
      this.length = requirePositive("length", length);
      return this;
    }

    @Override
    protected Builder self() {
      return this;
    }

    public GetObjectArgs build() {
      return new GetObjectArgs(this);
    }
  }
}
