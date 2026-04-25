package io.minio.reactive;

/** HEAD bucket 参数基类。 */
public abstract class HeadBucketBaseArgs extends BucketArgs {
  protected HeadBucketBaseArgs(BucketArgs.AbstractBuilder<?> builder) {
    super(builder);
  }
}
