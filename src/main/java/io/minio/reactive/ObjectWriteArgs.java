package io.minio.reactive;

/** 对象写入参数基类。 */
public abstract class ObjectWriteArgs extends ObjectArgs {
  protected ObjectWriteArgs(ObjectArgs.AbstractBuilder<?> builder) {
    super(builder);
  }
}
