package io.minio.reactive;

/** 列对象参数对象，覆盖最常用的 prefix 与 recursive 语义。 */
public final class ListObjectsArgs extends BucketArgs {
  private final String prefix;
  private final boolean recursive;

  private ListObjectsArgs(Builder builder) {
    super(builder);
    this.prefix = builder.prefix;
    this.recursive = builder.recursive;
  }

  public String prefix() {
    return prefix;
  }

  public boolean recursive() {
    return recursive;
  }

  public static Builder builder() {
    return new Builder();
  }

  public static final class Builder extends BucketArgs.AbstractBuilder<Builder> {
    private String prefix;
    private boolean recursive = true;

    public Builder prefix(String prefix) {
      this.prefix = prefix;
      return this;
    }

    public Builder recursive(boolean recursive) {
      this.recursive = recursive;
      return this;
    }

    @Override
    protected Builder self() {
      return this;
    }

    public ListObjectsArgs build() {
      return new ListObjectsArgs(this);
    }
  }
}
