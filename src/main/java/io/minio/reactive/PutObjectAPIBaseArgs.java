package io.minio.reactive;

/** PutObject API 参数基类。 */
public abstract class PutObjectAPIBaseArgs extends ObjectWriteArgs {
  protected PutObjectAPIBaseArgs(ObjectArgs.AbstractBuilder<?> builder) {
    super(builder);
  }
}
