package io.minio.reactive;

import io.minio.reactive.messages.ObjectRetentionConfiguration;

/** 设置对象保留策略的参数对象。 */
public final class SetObjectRetentionArgs extends ObjectVersionArgs {
  private final ObjectRetentionConfiguration configuration;

  private SetObjectRetentionArgs(Builder builder) {
    super(builder);
    if (builder.configuration == null) {
      throw new IllegalArgumentException("configuration 不能为空");
    }
    this.configuration = builder.configuration;
  }

  public ObjectRetentionConfiguration configuration() {
    return configuration;
  }

  public static Builder builder() {
    return new Builder();
  }

  public static final class Builder extends ObjectVersionArgs.AbstractBuilder<Builder> {
    private ObjectRetentionConfiguration configuration;

    public Builder configuration(ObjectRetentionConfiguration configuration) {
      this.configuration = configuration;
      return this;
    }

    @Override
    protected Builder self() {
      return this;
    }

    public SetObjectRetentionArgs build() {
      return new SetObjectRetentionArgs(this);
    }
  }
}
