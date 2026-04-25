package io.minio.reactive;

import java.util.Map;

/** 设置对象标签的参数对象。 */
public final class SetObjectTagsArgs extends ObjectVersionArgs {
  private final Map<String, String> tags;

  private SetObjectTagsArgs(Builder builder) {
    super(builder);
    this.tags = copyStringMap(builder.tags, "tags");
  }

  public Map<String, String> tags() {
    return tags;
  }

  public static Builder builder() {
    return new Builder();
  }

  public static final class Builder extends ObjectVersionArgs.AbstractBuilder<Builder> {
    private Map<String, String> tags;

    public Builder tags(Map<String, String> tags) {
      this.tags = tags;
      return this;
    }

    @Override
    protected Builder self() {
      return this;
    }

    public SetObjectTagsArgs build() {
      return new SetObjectTagsArgs(this);
    }
  }
}
