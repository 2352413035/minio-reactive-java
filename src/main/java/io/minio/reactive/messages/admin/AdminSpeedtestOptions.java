package io.minio.reactive.messages.admin;

import java.time.Duration;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 对象层 speedtest 参数。
 *
 * <p>MinIO madmin 对非 autotune 模式要求同时提供 duration、size 和 concurrency。
 * SDK 在本地提前校验这些参数，避免把明显危险或无意义的压测请求发送到服务端。
 */
public final class AdminSpeedtestOptions {
  private final int sizeBytes;
  private final int concurrency;
  private final Duration duration;
  private final boolean autotune;
  private final String storageClass;
  private final String bucket;
  private final boolean noClear;
  private final boolean enableSha256;
  private final boolean enableMultipart;

  private AdminSpeedtestOptions(Builder builder) {
    this.sizeBytes = builder.sizeBytes;
    this.concurrency = builder.concurrency;
    this.duration = builder.duration;
    this.autotune = builder.autotune;
    this.storageClass = trim(builder.storageClass);
    this.bucket = trim(builder.bucket);
    this.noClear = builder.noClear;
    this.enableSha256 = builder.enableSha256;
    this.enableMultipart = builder.enableMultipart;
    validate();
  }

  public static Builder builder() {
    return new Builder();
  }

  public int sizeBytes() {
    return sizeBytes;
  }

  public int concurrency() {
    return concurrency;
  }

  public Duration duration() {
    return duration;
  }

  public boolean autotune() {
    return autotune;
  }

  public String storageClass() {
    return storageClass;
  }

  public String bucket() {
    return bucket;
  }

  public boolean noClear() {
    return noClear;
  }

  public boolean enableSha256() {
    return enableSha256;
  }

  public boolean enableMultipart() {
    return enableMultipart;
  }

  /** 转成 MinIO madmin/server 识别的 query 参数。 */
  public Map<String, String> toQueryParameters() {
    Map<String, String> query = new LinkedHashMap<String, String>();
    if (sizeBytes > 0) {
      query.put("size", Integer.toString(sizeBytes));
    }
    if (duration != null) {
      query.put("duration", formatDuration(duration));
    }
    if (concurrency > 0) {
      query.put("concurrent", Integer.toString(concurrency));
    }
    if (!bucket.isEmpty()) {
      query.put("bucket", bucket);
    }
    if (!storageClass.isEmpty()) {
      query.put("storage-class", storageClass);
    }
    if (autotune) {
      query.put("autotune", "true");
    }
    if (noClear) {
      query.put("noclear", "true");
    }
    if (enableSha256) {
      query.put("enableSha256", "true");
    }
    if (enableMultipart) {
      query.put("enableMultipart", "true");
    }
    return Collections.unmodifiableMap(query);
  }

  /** 把 Java Duration 转成 Go time.ParseDuration 可接受的字符串。 */
  public static String formatDuration(Duration duration) {
    if (duration == null || duration.isZero() || duration.isNegative()) {
      throw new IllegalArgumentException("duration 必须大于 0");
    }
    long nanos = duration.toNanos();
    if (nanos % 1000000000L == 0) {
      return Long.toString(nanos / 1000000000L) + "s";
    }
    if (nanos % 1000000L == 0) {
      return Long.toString(nanos / 1000000L) + "ms";
    }
    if (nanos % 1000L == 0) {
      return Long.toString(nanos / 1000L) + "us";
    }
    return Long.toString(nanos) + "ns";
  }

  private void validate() {
    if (!autotune) {
      if (duration == null || duration.compareTo(Duration.ofSeconds(1)) <= 0) {
        throw new IllegalArgumentException("非 autotune speedtest 的 duration 必须大于 1 秒");
      }
      if (sizeBytes <= 0) {
        throw new IllegalArgumentException("非 autotune speedtest 的 sizeBytes 必须大于 0");
      }
      if (concurrency <= 0) {
        throw new IllegalArgumentException("非 autotune speedtest 的 concurrency 必须大于 0");
      }
    }
    if (sizeBytes < 0 || concurrency < 0) {
      throw new IllegalArgumentException("sizeBytes 和 concurrency 不能为负数");
    }
  }

  private static String trim(String value) {
    return value == null ? "" : value.trim();
  }

  public static final class Builder {
    private int sizeBytes;
    private int concurrency;
    private Duration duration;
    private boolean autotune;
    private String storageClass;
    private String bucket;
    private boolean noClear;
    private boolean enableSha256;
    private boolean enableMultipart;

    private Builder() {}

    public Builder sizeBytes(int sizeBytes) {
      this.sizeBytes = sizeBytes;
      return this;
    }

    public Builder concurrency(int concurrency) {
      this.concurrency = concurrency;
      return this;
    }

    public Builder duration(Duration duration) {
      this.duration = duration;
      return this;
    }

    public Builder autotune(boolean autotune) {
      this.autotune = autotune;
      return this;
    }

    public Builder storageClass(String storageClass) {
      this.storageClass = storageClass;
      return this;
    }

    public Builder bucket(String bucket) {
      this.bucket = bucket;
      return this;
    }

    public Builder noClear(boolean noClear) {
      this.noClear = noClear;
      return this;
    }

    public Builder enableSha256(boolean enableSha256) {
      this.enableSha256 = enableSha256;
      return this;
    }

    public Builder enableMultipart(boolean enableMultipart) {
      this.enableMultipart = enableMultipart;
      return this;
    }

    public AdminSpeedtestOptions build() {
      return new AdminSpeedtestOptions(this);
    }
  }
}
