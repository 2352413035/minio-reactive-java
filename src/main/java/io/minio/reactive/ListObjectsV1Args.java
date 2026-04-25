package io.minio.reactive;

/** `ListObjectsV1Args`：列表类请求参数对象。 */
public final class ListObjectsV1Args extends BucketArgs {
  private final String prefix;
  private final boolean recursive;

  private ListObjectsV1Args(Builder builder) {
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

    public ListObjectsV1Args build() {
      return new ListObjectsV1Args(this);
    }
  }
}
