package io.minio.reactive;

import io.minio.reactive.messages.RestoreObjectRequest;

/** 恢复归档对象的参数对象。 */
public final class RestoreObjectArgs extends ObjectVersionArgs {
  private final RestoreObjectRequest restoreRequest;

  private RestoreObjectArgs(Builder builder) {
    super(builder);
    if (builder.restoreRequest == null) {
      throw new IllegalArgumentException("restoreRequest 不能为空");
    }
    this.restoreRequest = builder.restoreRequest;
  }

  public RestoreObjectRequest restoreRequest() {
    return restoreRequest;
  }

  public static Builder builder() {
    return new Builder();
  }

  public static final class Builder extends ObjectVersionArgs.AbstractBuilder<Builder> {
    private RestoreObjectRequest restoreRequest;

    public Builder restoreRequest(RestoreObjectRequest restoreRequest) {
      this.restoreRequest = restoreRequest;
      return this;
    }

    @Override
    protected Builder self() {
      return this;
    }

    public RestoreObjectArgs build() {
      return new RestoreObjectArgs(this);
    }
  }
}
