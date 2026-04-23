package io.minio.reactive.errors;

/** Health 协议族错误。 */
public final class ReactiveMinioHealthException extends ReactiveMinioException {
  public ReactiveMinioHealthException(
      int statusCode,
      String code,
      String errorMessage,
      String requestId,
      String rawBody) {
    super("health", statusCode, code, errorMessage, requestId, rawBody);
  }

  public ReactiveMinioHealthException(
      int statusCode,
      String code,
      String errorMessage,
      String requestId,
      String rawBody,
      String endpointName,
      String method,
      String path,
      String diagnosticHint) {
    super("health", statusCode, code, errorMessage, requestId, rawBody, endpointName, method, path, diagnosticHint);
  }
}
