package io.minio.reactive;

/** 对象条件读取参数基类。 */
public abstract class ObjectConditionalReadArgs extends ObjectReadArgs {
  protected ObjectConditionalReadArgs(ObjectVersionArgs.AbstractBuilder<?> builder) {
    super(builder);
  }
}
