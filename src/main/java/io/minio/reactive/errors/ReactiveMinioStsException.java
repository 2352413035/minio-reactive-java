package io.minio.reactive.errors;

/** Sts 协议族错误。 */
public final class ReactiveMinioStsException extends ReactiveMinioException {
  public ReactiveMinioStsException(
      int statusCode,
      String code,
      String errorMessage,
      String requestId,
      String rawBody) {
    super("sts", statusCode, code, errorMessage, requestId, rawBody);
  }
}
