package io.minio.reactive;

import java.nio.charset.StandardCharsets;

/** 追加对象参数对象，当前迁移层先覆盖内存字节与字符串内容。 */
public final class AppendObjectArgs extends ObjectArgs {
  private final byte[] content;
  private final String contentType;

  private AppendObjectArgs(Builder builder) {
    super(builder);
    this.content = builder.content == null ? new byte[0] : builder.content.clone();
    this.contentType = builder.contentType;
  }

  public byte[] content() {
    return content.clone();
  }

  public String contentType() {
    return contentType;
  }

  public static Builder builder() {
    return new Builder();
  }

  public static final class Builder extends ObjectArgs.AbstractBuilder<Builder> {
    private byte[] content;
    private String contentType;

    public Builder content(byte[] content) {
      this.content = content == null ? new byte[0] : content.clone();
      return this;
    }

    public Builder content(String content) {
      this.content = (content == null ? "" : content).getBytes(StandardCharsets.UTF_8);
      return this;
    }

    public Builder contentType(String contentType) {
      this.contentType = contentType;
      return this;
    }

    @Override
    protected Builder self() {
      return this;
    }

    public AppendObjectArgs build() {
      return new AppendObjectArgs(this);
    }
  }
}
