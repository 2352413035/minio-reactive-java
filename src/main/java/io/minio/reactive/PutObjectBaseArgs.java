package io.minio.reactive;

/** PutObject 内容参数基类。 */
public abstract class PutObjectBaseArgs extends PutObjectAPIArgs {
  protected PutObjectBaseArgs(ObjectArgs.AbstractBuilder<?> builder) {
    super(builder);
  }
}
