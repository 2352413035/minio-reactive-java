package io.minio.reactive;

import java.nio.file.Path;
import java.nio.file.Paths;

/** 下载对象到本地文件的参数对象。 */
public final class DownloadObjectArgs extends ObjectArgs {
  private final Path filename;
  private final boolean overwrite;

  private DownloadObjectArgs(Builder builder) {
    super(builder);
    if (builder.filename == null) {
      throw new IllegalArgumentException("filename 不能为空");
    }
    this.filename = builder.filename;
    this.overwrite = builder.overwrite;
  }

  public Path filename() {
    return filename;
  }

  public boolean overwrite() {
    return overwrite;
  }

  public static Builder builder() {
    return new Builder();
  }

  public static final class Builder extends ObjectArgs.AbstractBuilder<Builder> {
    private Path filename;
    private boolean overwrite;

    public Builder filename(Path filename) {
      this.filename = filename;
      return this;
    }

    public Builder filename(String filename) {
      this.filename = filename == null ? null : Paths.get(filename);
      return this;
    }

    public Builder overwrite(boolean overwrite) {
      this.overwrite = overwrite;
      return this;
    }

    @Override
    protected Builder self() {
      return this;
    }

    public DownloadObjectArgs build() {
      return new DownloadObjectArgs(this);
    }
  }
}
