package io.minio.reactive;

/** PutObject API 参数对象基类。 */
public abstract class PutObjectAPIArgs extends PutObjectAPIBaseArgs {
  protected PutObjectAPIArgs(ObjectArgs.AbstractBuilder<?> builder) {
    super(builder);
  }
}
