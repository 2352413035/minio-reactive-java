package io.minio.reactive;

/** HEAD object 参数基类。 */
public abstract class HeadObjectBaseArgs extends ObjectArgs {
  protected HeadObjectBaseArgs(ObjectArgs.AbstractBuilder<?> builder) {
    super(builder);
  }
}
