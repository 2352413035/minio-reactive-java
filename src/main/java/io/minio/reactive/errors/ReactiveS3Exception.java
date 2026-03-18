package io.minio.reactive.errors;

/**
 * Basic S3 HTTP error wrapper for the reactive client.
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
