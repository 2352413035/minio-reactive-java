package io.minio.reactive;

import java.time.Duration;
import org.springframework.http.HttpMethod;

/** 生成对象预签名 URL 的参数对象。 */
public final class GetPresignedObjectUrlArgs extends ObjectArgs {
  private final HttpMethod method;
  private final Duration expiry;

  private GetPresignedObjectUrlArgs(Builder builder) {
    super(builder);
    this.method = builder.method == null ? HttpMethod.GET : builder.method;
    this.expiry = builder.expiry;
  }

  public HttpMethod method() {
    return method;
  }

  public Duration expiry() {
    return expiry;
  }

  public static Builder builder() {
    return new Builder();
  }

  public static final class Builder extends ObjectArgs.AbstractBuilder<Builder> {
    private HttpMethod method;
    private Duration expiry;

    public Builder method(HttpMethod method) {
      this.method = method;
      return this;
    }

    public Builder expiry(Duration expiry) {
      this.expiry = expiry;
      return this;
    }

    @Override
    protected Builder self() {
      return this;
    }

    public GetPresignedObjectUrlArgs build() {
      return new GetPresignedObjectUrlArgs(this);
    }
  }
}
