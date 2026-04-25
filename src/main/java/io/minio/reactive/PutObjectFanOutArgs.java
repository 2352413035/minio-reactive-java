package io.minio.reactive;

import io.minio.reactive.messages.PutObjectFanOutEntry;
import java.nio.charset.StandardCharsets;
import java.util.List;

/** FanOut 上传参数对象。 */
public final class PutObjectFanOutArgs extends BucketArgs {
  private final byte[] content;
  private final List<PutObjectFanOutEntry> entries;
  private final String contentType;

  private PutObjectFanOutArgs(Builder builder) {
    super(builder);
    this.content = builder.content == null ? new byte[0] : builder.content.clone();
    this.entries = copyList(builder.entries, "entries");
    this.contentType = builder.contentType;
  }

  public byte[] content() {
    return content.clone();
  }

  public List<PutObjectFanOutEntry> entries() {
    return entries;
  }

  public String contentType() {
    return contentType;
  }

  public static Builder builder() {
    return new Builder();
  }

  public static final class Builder extends BucketArgs.AbstractBuilder<Builder> {
    private byte[] content;
    private Iterable<PutObjectFanOutEntry> entries;
    private String contentType;

    public Builder content(byte[] content) {
      this.content = content == null ? new byte[0] : content.clone();
      return this;
    }

    public Builder content(String content) {
      this.content = (content == null ? "" : content).getBytes(StandardCharsets.UTF_8);
      return this;
    }

    public Builder entries(Iterable<PutObjectFanOutEntry> entries) {
      this.entries = entries;
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

    public PutObjectFanOutArgs build() {
      return new PutObjectFanOutArgs(this);
    }
  }
}
