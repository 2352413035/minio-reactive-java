package io.minio.reactive.errors;

import io.minio.reactive.messages.S3Error;
import io.minio.reactive.util.S3Xml;

/**
 * 响应式 MinIO 客户端的基础 S3 异常。
 *
 * <p>除了 HTTP 状态码和原始响应体，这里也尽量解析 S3 XML 错误字段，方便调用方按
 * {@code NoSuchBucket}、{@code NoSuchKey} 等协议错误码做分支处理。
 */
public final class ReactiveS3Exception extends ReactiveMinioException {
  private final String responseBody;
  private final S3Error s3Error;

  public ReactiveS3Exception(int statusCode, String responseBody) {
    this(statusCode, responseBody, S3Xml.parseError(responseBody), "");
  }

  public ReactiveS3Exception(int statusCode, String responseBody, String requestId) {
    this(statusCode, responseBody, S3Xml.parseError(responseBody), requestId);
  }

  public ReactiveS3Exception(int statusCode, String responseBody, S3Error s3Error) {
    this(statusCode, responseBody, s3Error, s3Error == null ? "" : s3Error.requestId());
  }

  public ReactiveS3Exception(int statusCode, String responseBody, S3Error s3Error, String requestId) {
    super(
        "s3",
        statusCode,
        s3Error == null ? "" : s3Error.code(),
        s3Error == null ? "" : s3Error.message(),
        requestId == null || requestId.isEmpty()
            ? (s3Error == null ? "" : s3Error.requestId())
            : requestId,
        responseBody);
    this.responseBody = responseBody;
    this.s3Error = s3Error;
  }

  public String responseBody() {
    return responseBody;
  }

  public S3Error s3Error() {
    return s3Error;
  }

  public String errorCode() {
    return s3Error == null ? code() : s3Error.code();
  }

  public String errorMessage() {
    return s3Error == null ? super.errorMessage() : s3Error.message();
  }
}
