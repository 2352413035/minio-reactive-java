package io.minio.reactive.errors;

import io.minio.reactive.messages.S3Error;
import io.minio.reactive.util.S3Xml;

/**
 * 响应式 MinIO 客户端的基础 S3 异常。
 *
 * <p>除了 HTTP 状态码和原始响应体，这里也尽量解析 S3 XML 错误字段，方便调用方按
 * {@code NoSuchBucket}、{@code NoSuchKey} 等协议错误码做分支处理。
 */
public final class ReactiveS3Exception extends RuntimeException {
  private final int statusCode;
  private final String responseBody;
  private final S3Error s3Error;

  public ReactiveS3Exception(int statusCode, String responseBody) {
    this(statusCode, responseBody, S3Xml.parseError(responseBody));
  }

  public ReactiveS3Exception(int statusCode, String responseBody, S3Error s3Error) {
    super(buildMessage(statusCode, responseBody, s3Error));
    this.statusCode = statusCode;
    this.responseBody = responseBody;
    this.s3Error = s3Error;
  }

  public int statusCode() {
    return statusCode;
  }

  public String responseBody() {
    return responseBody;
  }

  public S3Error s3Error() {
    return s3Error;
  }

  public String errorCode() {
    return s3Error == null ? "" : s3Error.code();
  }

  public String errorMessage() {
    return s3Error == null ? "" : s3Error.message();
  }

  private static String buildMessage(int statusCode, String responseBody, S3Error s3Error) {
    if (s3Error != null && s3Error.code() != null && !s3Error.code().isEmpty()) {
      String message = s3Error.message() == null ? "" : s3Error.message();
      return "S3 request failed with HTTP status "
          + statusCode
          + ", code="
          + s3Error.code()
          + ", message="
          + message;
    }

    String body = responseBody == null ? "" : responseBody.trim();
    if (body.isEmpty()) {
      return "S3 request failed with HTTP status " + statusCode;
    }

    String singleLine = body.replaceAll("\\s+", " ");
    if (singleLine.length() > 600) {
      singleLine = singleLine.substring(0, 600) + "...";
    }
    return "S3 request failed with HTTP status " + statusCode + ", body=" + singleLine;
  }
}
