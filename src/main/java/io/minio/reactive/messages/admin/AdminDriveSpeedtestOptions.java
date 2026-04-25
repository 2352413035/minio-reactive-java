package io.minio.reactive.messages.admin;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 磁盘层 speedtest 参数。
 *
 * <p>MinIO 服务端默认 file size 较大；SDK 要求调用方显式传入较小的 blockSizeBytes/fileSizeBytes，
 * 避免因为漏填参数触发服务端 1GiB 默认压测。
 */
public final class AdminDriveSpeedtestOptions {
  private final boolean serial;
  private final long blockSizeBytes;
  private final long fileSizeBytes;

  private AdminDriveSpeedtestOptions(Builder builder) {
    this.serial = builder.serial;
    this.blockSizeBytes = builder.blockSizeBytes;
    this.fileSizeBytes = builder.fileSizeBytes;
    validate();
  }

  public static Builder builder() {
    return new Builder();
  }

  public boolean serial() {
    return serial;
  }

  public long blockSizeBytes() {
    return blockSizeBytes;
  }

  public long fileSizeBytes() {
    return fileSizeBytes;
  }

  /** 转成 MinIO madmin/server 识别的 query 参数。 */
  public Map<String, String> toQueryParameters() {
    Map<String, String> query = new LinkedHashMap<String, String>();
    if (serial) {
      query.put("serial", "true");
    }
    if (blockSizeBytes > 0) {
      query.put("blocksize", Long.toString(blockSizeBytes));
    }
    if (fileSizeBytes > 0) {
      query.put("filesize", Long.toString(fileSizeBytes));
    }
    return Collections.unmodifiableMap(query);
  }

  private void validate() {
    if (blockSizeBytes <= 0 || fileSizeBytes <= 0) {
      throw new IllegalArgumentException(
          "drive speedtest 的 blockSizeBytes 和 fileSizeBytes 必须大于 0，避免触发服务端默认大文件压测");
    }
  }

  public static final class Builder {
    private boolean serial;
    private long blockSizeBytes;
    private long fileSizeBytes;

    private Builder() {}

    public Builder serial(boolean serial) {
      this.serial = serial;
      return this;
    }

    public Builder blockSizeBytes(long blockSizeBytes) {
      this.blockSizeBytes = blockSizeBytes;
      return this;
    }

    public Builder fileSizeBytes(long fileSizeBytes) {
      this.fileSizeBytes = fileSizeBytes;
      return this;
    }

    public AdminDriveSpeedtestOptions build() {
      return new AdminDriveSpeedtestOptions(this);
    }
  }
}
