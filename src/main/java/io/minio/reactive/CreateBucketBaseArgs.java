package io.minio.reactive;

/** 创建 bucket 参数基类。 */
public abstract class CreateBucketBaseArgs extends BucketArgs {
  protected CreateBucketBaseArgs(BucketArgs.AbstractBuilder<?> builder) {
    super(builder);
  }
}
