package io.minio.reactive;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/** 获取对象属性的参数对象。 */
public final class GetObjectAttributesArgs extends ObjectArgs {
  private final List<String> attributes;

  private GetObjectAttributesArgs(Builder builder) {
    super(builder);
    this.attributes = builder.attributes == null
        ? Collections.<String>emptyList()
        : Collections.unmodifiableList(builder.attributes);
  }

  public List<String> attributes() {
    return attributes;
  }

  public static Builder builder() {
    return new Builder();
  }

  public static final class Builder extends ObjectArgs.AbstractBuilder<Builder> {
    private List<String> attributes;

    public Builder attributes(String... attributes) {
      this.attributes = attributes == null ? null : Arrays.asList(attributes);
      return this;
    }

    @Override
    protected Builder self() {
      return this;
    }

    public GetObjectAttributesArgs build() {
      return new GetObjectAttributesArgs(this);
    }
  }
}
