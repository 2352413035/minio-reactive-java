package io.minio.reactive;

import java.util.List;

/** 批量删除对象参数对象。 */
public final class RemoveObjectsArgs extends BucketArgs {
  private final List<String> objects;

  private RemoveObjectsArgs(Builder builder) {
    super(builder);
    this.objects = copyList(builder.objects, "objects");
  }

  public List<String> objects() {
    return objects;
  }

  public static Builder builder() {
    return new Builder();
  }

  public static final class Builder extends BucketArgs.AbstractBuilder<Builder> {
    private Iterable<String> objects;

    public Builder objects(Iterable<String> objects) {
      this.objects = objects;
      return this;
    }

    @Override
    protected Builder self() {
      return this;
    }

    public RemoveObjectsArgs build() {
      return new RemoveObjectsArgs(this);
    }
  }
}
