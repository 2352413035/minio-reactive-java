package io.minio.reactive;

import java.util.Map;

/** 设置 bucket 标签的参数对象。 */
public final class SetBucketTagsArgs extends BucketArgs {
  private final Map<String, String> tags;

  private SetBucketTagsArgs(Builder builder) {
    super(builder);
    this.tags = copyStringMap(builder.tags, "tags");
  }

  public Map<String, String> tags() {
    return tags;
  }

  public static Builder builder() {
    return new Builder();
  }

  public static final class Builder extends BucketArgs.AbstractBuilder<Builder> {
    private Map<String, String> tags;

    public Builder tags(Map<String, String> tags) {
      this.tags = tags;
      return this;
    }

    @Override
    protected Builder self() {
      return this;
    }

    public SetBucketTagsArgs build() {
      return new SetBucketTagsArgs(this);
    }
  }
}
