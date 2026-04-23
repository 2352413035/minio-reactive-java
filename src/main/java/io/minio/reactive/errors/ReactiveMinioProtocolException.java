package io.minio.reactive.errors;

/**
 * 非 S3 协议族的 MinIO 错误。
 *
 * <p>Admin、KMS、STS、Metrics、Health 的错误格式与 S3 XML 不完全一致，使用这个异常可以保留
 * 协议名称、HTTP 状态、错误码、错误消息、requestId 和原始响应体。
 */
public final class ReactiveMinioProtocolException extends ReactiveMinioException {
  public ReactiveMinioProtocolException(
      String protocol,
      int statusCode,
      String code,
      String errorMessage,
      String requestId,
      String rawBody) {
    super(protocol, statusCode, code, errorMessage, requestId, rawBody);
  }

  public ReactiveMinioProtocolException(
      String protocol,
      int statusCode,
      String code,
      String errorMessage,
      String requestId,
      String rawBody,
      String endpointName,
      String method,
      String path,
      String diagnosticHint) {
    super(protocol, statusCode, code, errorMessage, requestId, rawBody, endpointName, method, path, diagnosticHint);
  }
}
