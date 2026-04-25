package io.minio.reactive;

import java.nio.file.Path;
import java.nio.file.Paths;

/** 上传本地文件的参数对象。 */
public final class UploadObjectArgs extends ObjectArgs {
  private final Path filename;
  private final String contentType;

  private UploadObjectArgs(Builder builder) {
    super(builder);
    if (builder.filename == null) {
      throw new IllegalArgumentException("filename 不能为空");
    }
    this.filename = builder.filename;
    this.contentType = builder.contentType;
  }

  public Path filename() {
    return filename;
  }

  public String contentType() {
    return contentType;
  }

  public static Builder builder() {
    return new Builder();
  }

  public static final class Builder extends ObjectArgs.AbstractBuilder<Builder> {
    private Path filename;
    private String contentType;

    public Builder filename(Path filename) {
      this.filename = filename;
      return this;
    }

    public Builder filename(String filename) {
      this.filename = filename == null ? null : Paths.get(filename);
      return this;
    }

    public Builder contentType(String contentType) {
      this.contentType = contentType;
      return this;
    }

    @Override
    protected Builder self() {
      return this;
    }

    public UploadObjectArgs build() {
      return new UploadObjectArgs(this);
    }
  }
}
