package io.minio.reactive.errors;

/** Admin 协议族错误。 */
public final class ReactiveMinioAdminException extends ReactiveMinioException {
  public ReactiveMinioAdminException(
      int statusCode,
      String code,
      String errorMessage,
      String requestId,
      String rawBody) {
    super("admin", statusCode, code, errorMessage, requestId, rawBody);
  }

  public ReactiveMinioAdminException(
      int statusCode,
      String code,
      String errorMessage,
      String requestId,
      String rawBody,
      String endpointName,
      String method,
      String path,
      String diagnosticHint) {
    super("admin", statusCode, code, errorMessage, requestId, rawBody, endpointName, method, path, diagnosticHint);
  }
}
