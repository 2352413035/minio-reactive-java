package io.minio.reactive;

/** 列出 bucket 的 Args；保留 builder 形式以便从 minio-java 迁移。 */
public final class ListBucketsArgs extends BaseArgs {
  private ListBucketsArgs() {}

  public static Builder builder() {
    return new Builder();
  }

  public static final class Builder {
    public ListBucketsArgs build() {
      return new ListBucketsArgs();
    }
  }
}
