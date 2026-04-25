package io.minio.reactive;

import io.minio.reactive.messages.ComposeSource;
import java.util.List;

/** 组合对象参数对象。 */
public final class ComposeObjectArgs extends ObjectArgs {
  private final List<ComposeSource> sources;
  private final String contentType;

  private ComposeObjectArgs(Builder builder) {
    super(builder);
    this.sources = copyList(builder.sources, "sources");
    this.contentType = builder.contentType;
  }

  public List<ComposeSource> sources() {
    return sources;
  }

  public String contentType() {
    return contentType;
  }

  public static Builder builder() {
    return new Builder();
  }

  public static final class Builder extends ObjectArgs.AbstractBuilder<Builder> {
    private Iterable<ComposeSource> sources;
    private String contentType;

    public Builder sources(Iterable<ComposeSource> sources) {
      this.sources = sources;
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

    public ComposeObjectArgs build() {
      return new ComposeObjectArgs(this);
    }
  }
}
