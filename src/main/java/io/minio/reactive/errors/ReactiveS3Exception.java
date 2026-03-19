package io.minio.reactive.errors;

/**
 * 响应式 MinIO 客户端的基础 S3 异常。
 *
 * <p>当前版本先保留最核心的两项信息：
 *
 * <ul>
 *   <li>HTTP 状态码
 *   <li>响应体文本
 * </ul>
 *
 * <p>这样在排查 403、404、XML 错误响应时，调用方至少能直接看到服务端返回了什么。
 */
public final class ReactiveS3Exception extends RuntimeException {
  private final int statusCode;
  private final String responseBody;

  public ReactiveS3Exception(int statusCode, String responseBody) {
    super(buildMessage(statusCode, responseBody));
    this.statusCode = statusCode;
    this.responseBody = responseBody;
  }

  public int statusCode() {
    return statusCode;
  }

  public String responseBody() {
    return responseBody;
  }

  private static String buildMessage(int statusCode, String responseBody) {
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
