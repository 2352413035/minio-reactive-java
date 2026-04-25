package io.minio.reactive;

/** 对象读取参数基类。 */
public abstract class ObjectReadArgs extends ObjectVersionArgs {
  protected ObjectReadArgs(ObjectVersionArgs.AbstractBuilder<?> builder) {
    super(builder);
  }
}
