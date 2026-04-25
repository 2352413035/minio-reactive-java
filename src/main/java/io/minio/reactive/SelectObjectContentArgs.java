package io.minio.reactive;

import io.minio.reactive.messages.SelectObjectContentRequest;

/** S3 Select 请求参数对象。 */
public final class SelectObjectContentArgs extends ObjectArgs {
  private final SelectObjectContentRequest selectRequest;

  private SelectObjectContentArgs(Builder builder) {
    super(builder);
    if (builder.selectRequest == null) {
      throw new IllegalArgumentException("selectRequest 不能为空");
    }
    this.selectRequest = builder.selectRequest;
  }

  public SelectObjectContentRequest selectRequest() {
    return selectRequest;
  }

  public static Builder builder() {
    return new Builder();
  }

  public static final class Builder extends ObjectArgs.AbstractBuilder<Builder> {
    private SelectObjectContentRequest selectRequest;

    public Builder selectRequest(SelectObjectContentRequest selectRequest) {
      this.selectRequest = selectRequest;
      return this;
    }

    @Override
    protected Builder self() {
      return this;
    }

    public SelectObjectContentArgs build() {
      return new SelectObjectContentArgs(this);
    }
  }
}
