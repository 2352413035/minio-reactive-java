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
}
