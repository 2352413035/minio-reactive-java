package io.minio.reactive.messages;

import io.minio.reactive.util.S3Escaper;

/**
 * `composeObject` 使用的源对象描述。
 *
 * <p>它对应 minio-java 的 ComposeSource 思路，但保持本 SDK 的轻量不可变模型：
 * 必须给出源 bucket 和 object，可选 versionId 与字节范围。字节范围最终会变成
 * `x-amz-copy-source-range` 头，用于只组合源对象的一部分内容。
 */
public final class ComposeSource {
  private final String bucket;
  private final String object;
  private final String versionId;
  private final Long startInclusive;
  private final Long endInclusive;

  private ComposeSource(
      String bucket, String object, String versionId, Long startInclusive, Long endInclusive) {
    this.bucket = requireNonBlank(bucket, "源 bucket 不能为空");
    this.object = requireNonBlank(object, "源 object 不能为空");
    this.versionId = emptyToNull(versionId);
    if ((startInclusive == null) != (endInclusive == null)) {
      throw new IllegalArgumentException("源对象范围必须同时提供开始和结束位置");
    }
    if (startInclusive != null
        && (startInclusive.longValue() < 0L
            || endInclusive.longValue() < startInclusive.longValue())) {
      throw new IllegalArgumentException("源对象范围无效");
    }
    this.startInclusive = startInclusive;
    this.endInclusive = endInclusive;
  }

  public static ComposeSource of(String bucket, String object) {
    return new ComposeSource(bucket, object, null, null, null);
  }

  public static ComposeSource of(String bucket, String object, String versionId) {
    return new ComposeSource(bucket, object, versionId, null, null);
  }

  public ComposeSource withRange(long startInclusive, long endInclusive) {
    return new ComposeSource(bucket, object, versionId, startInclusive, endInclusive);
  }

  public String bucket() {
    return bucket;
  }

  public String object() {
    return object;
  }

  public String versionId() {
    return versionId;
  }

  public Long startInclusive() {
    return startInclusive;
  }

  public Long endInclusive() {
    return endInclusive;
  }

  public boolean hasRange() {
    return startInclusive != null;
  }

  /** 生成 S3 multipart copy 所需的 `x-amz-copy-source` 头。 */
  public String copySourceHeader() {
    String value = S3Escaper.canonicalUri(bucket, object);
    if (versionId != null) {
      value = value + "?versionId=" + S3Escaper.encodeQueryComponent(versionId);
    }
    return value;
  }

  /** 生成 S3 multipart copy 所需的可选范围头。 */
  public String copySourceRangeHeader() {
    if (!hasRange()) {
      return null;
    }
    return "bytes=" + startInclusive + "-" + endInclusive;
  }

  private static String requireNonBlank(String value, String message) {
    if (value == null || value.trim().isEmpty()) {
      throw new IllegalArgumentException(message);
    }
    return value;
  }

  private static String emptyToNull(String value) {
    return value == null || value.trim().isEmpty() ? null : value;
  }
}
