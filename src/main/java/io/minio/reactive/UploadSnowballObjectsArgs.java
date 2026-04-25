package io.minio.reactive;

import io.minio.reactive.messages.SnowballObject;
import java.util.List;

/** Snowball 批量上传参数对象。 */
public final class UploadSnowballObjectsArgs extends BucketArgs {
  private final List<SnowballObject> objects;
  private final boolean compression;

  private UploadSnowballObjectsArgs(Builder builder) {
    super(builder);
    this.objects = copyList(builder.objects, "objects");
    this.compression = builder.compression;
  }

  public List<SnowballObject> objects() {
    return objects;
  }

  public boolean compression() {
    return compression;
  }

  public static Builder builder() {
    return new Builder();
  }

  public static final class Builder extends BucketArgs.AbstractBuilder<Builder> {
    private Iterable<SnowballObject> objects;
    private boolean compression;

    public Builder objects(Iterable<SnowballObject> objects) {
      this.objects = objects;
      return this;
    }

    public Builder compression(boolean compression) {
      this.compression = compression;
      return this;
    }

    @Override
    protected Builder self() {
      return this;
    }

    public UploadSnowballObjectsArgs build() {
      return new UploadSnowballObjectsArgs(this);
    }
  }
}
