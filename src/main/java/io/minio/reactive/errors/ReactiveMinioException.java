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
  private final String endpointName;
  private final String method;
  private final String path;
  private final String diagnosticHint;

  public ReactiveMinioException(
      String protocol,
      int statusCode,
      String code,
      String errorMessage,
      String requestId,
      String rawBody) {
    this(protocol, statusCode, code, errorMessage, requestId, rawBody, "", "", "", "");
  }

  public ReactiveMinioException(
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
    super(buildMessage(protocol, statusCode, code, errorMessage, requestId, rawBody, endpointName, method, path, diagnosticHint));
    this.protocol = protocol;
    this.statusCode = statusCode;
    this.code = code == null ? "" : code;
    this.errorMessage = errorMessage == null ? "" : errorMessage;
    this.requestId = requestId == null ? "" : requestId;
    this.rawBody = rawBody == null ? "" : rawBody;
    this.endpointName = endpointName == null ? "" : endpointName;
    this.method = method == null ? "" : method;
    this.path = path == null ? "" : path;
    this.diagnosticHint = diagnosticHint == null ? "" : diagnosticHint;
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

  public String endpointName() {
    return endpointName;
  }

  public String method() {
    return method;
  }

  public String path() {
    return path;
  }

  public String diagnosticHint() {
    return diagnosticHint;
  }

  private static String buildMessage(
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
    StringBuilder builder = new StringBuilder();
    builder.append("MinIO ");
    builder.append(protocol == null || protocol.isEmpty() ? "请求" : protocol + " 请求");
    builder.append("失败，HTTP 状态=").append(statusCode);
    if (code != null && !code.isEmpty()) {
      builder.append("，错误码=").append(code);
    }
    if (errorMessage != null && !errorMessage.isEmpty()) {
      builder.append("，错误信息=").append(errorMessage);
    }
    if (requestId != null && !requestId.isEmpty()) {
      builder.append("，请求ID=").append(requestId);
    }
    if (endpointName != null && !endpointName.isEmpty()) {
      builder.append("，接口=").append(endpointName);
    }
    if (method != null && !method.isEmpty()) {
      builder.append("，方法=").append(method);
    }
    if (path != null && !path.isEmpty()) {
      builder.append("，路径=").append(path);
    }
    if (diagnosticHint != null && !diagnosticHint.isEmpty()) {
      builder.append("，诊断建议=").append(diagnosticHint);
    }
    if ((code == null || code.isEmpty()) && (errorMessage == null || errorMessage.isEmpty())) {
      String body = rawBody == null ? "" : rawBody.trim().replaceAll("\\s+", " ");
      if (!body.isEmpty()) {
        builder.append("，响应体片段=").append(body.length() > 600 ? body.substring(0, 600) + "..." : body);
      }
    }
    return builder.toString();
  }
}
