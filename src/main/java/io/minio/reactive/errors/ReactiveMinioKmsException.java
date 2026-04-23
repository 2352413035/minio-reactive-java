package io.minio.reactive.errors;

/** Kms 协议族错误。 */
public final class ReactiveMinioKmsException extends ReactiveMinioException {
  public ReactiveMinioKmsException(
      int statusCode,
      String code,
      String errorMessage,
      String requestId,
      String rawBody) {
    super("kms", statusCode, code, errorMessage, requestId, rawBody);
  }

  public ReactiveMinioKmsException(
      int statusCode,
      String code,
      String errorMessage,
      String requestId,
      String rawBody,
      String endpointName,
      String method,
      String path,
      String diagnosticHint) {
    super("kms", statusCode, code, errorMessage, requestId, rawBody, endpointName, method, path, diagnosticHint);
  }
}
