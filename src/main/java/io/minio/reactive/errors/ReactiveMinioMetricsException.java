package io.minio.reactive.errors;

/** Metrics 协议族错误。 */
public final class ReactiveMinioMetricsException extends ReactiveMinioException {
  public ReactiveMinioMetricsException(
      int statusCode,
      String code,
      String errorMessage,
      String requestId,
      String rawBody) {
    super("metrics", statusCode, code, errorMessage, requestId, rawBody);
  }

  public ReactiveMinioMetricsException(
      int statusCode,
      String code,
      String errorMessage,
      String requestId,
      String rawBody,
      String endpointName,
      String method,
      String path,
      String diagnosticHint) {
    super("metrics", statusCode, code, errorMessage, requestId, rawBody, endpointName, method, path, diagnosticHint);
  }
}
