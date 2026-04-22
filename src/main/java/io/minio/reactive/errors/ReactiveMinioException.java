package io.minio.reactive.errors;

/**
 * MinIO 响应式 SDK 的通用异常基类。
 *
 * <p>不同协议族的错误格式不同：S3 常见 XML，Admin/KMS 常见 JSON 或文本，Metrics 可能是文本，
 * Health 可能只有状态码。这个基类只承载所有协议都需要的诊断字段，具体协议再派生更细的异常。
 */
public class ReactiveMinioException extends RuntimeException {
  private final String protocol;
  private final int statusCode;
  private final String code;
  private final String errorMessage;
  private final String requestId;
  private final String rawBody;

  public ReactiveMinioException(
      String protocol,
      int statusCode,
      String code,
      String errorMessage,
      String requestId,
      String rawBody) {
    super(buildMessage(protocol, statusCode, code, errorMessage, requestId, rawBody));
    this.protocol = protocol;
    this.statusCode = statusCode;
    this.code = code == null ? "" : code;
    this.errorMessage = errorMessage == null ? "" : errorMessage;
    this.requestId = requestId == null ? "" : requestId;
    this.rawBody = rawBody == null ? "" : rawBody;
  }

  public String protocol() {
    return protocol;
  }

  public int statusCode() {
    return statusCode;
  }

  public String code() {
    return code;
  }

  public String errorMessage() {
    return errorMessage;
  }

  public String requestId() {
    return requestId;
  }

  public String rawBody() {
    return rawBody;
  }

  private static String buildMessage(
      String protocol,
      int statusCode,
      String code,
      String errorMessage,
      String requestId,
      String rawBody) {
    StringBuilder builder = new StringBuilder();
    builder.append(protocol == null || protocol.isEmpty() ? "MinIO" : protocol);
    builder.append(" request failed with HTTP status ").append(statusCode);
    if (code != null && !code.isEmpty()) {
      builder.append(", code=").append(code);
    }
    if (errorMessage != null && !errorMessage.isEmpty()) {
      builder.append(", message=").append(errorMessage);
    }
    if (requestId != null && !requestId.isEmpty()) {
      builder.append(", requestId=").append(requestId);
    }
    if ((code == null || code.isEmpty()) && (errorMessage == null || errorMessage.isEmpty())) {
      String body = rawBody == null ? "" : rawBody.trim().replaceAll("\\s+", " ");
      if (!body.isEmpty()) {
        builder.append(", body=").append(body.length() > 600 ? body.substring(0, 600) + "..." : body);
      }
    }
    return builder.toString();
  }
}
