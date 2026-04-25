package io.minio.reactive;

/** 带 bucket 名称的 Args 基类，供具体 bucket 请求对象继承。 */
public abstract class BucketArgs extends BaseArgs {
  private final String bucket;

  protected BucketArgs(AbstractBuilder<?> builder) {
    this.bucket = requireText("bucket", builder.bucket);
  }

  public String bucket() {
    return bucket;
  }

  public abstract static class AbstractBuilder<B extends AbstractBuilder<B>> {
    private String bucket;

    public B bucket(String bucket) {
      this.bucket = bucket;
      return self();
    }

    protected abstract B self();
  }
}
