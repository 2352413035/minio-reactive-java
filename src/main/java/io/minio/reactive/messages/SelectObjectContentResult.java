package io.minio.reactive.messages;

/** S3 SelectObjectContent 当前阶段的响应边界。 */
public final class SelectObjectContentResult {
  private final String rawResponse;

  public SelectObjectContentResult(String rawResponse) {
    this.rawResponse = rawResponse == null ? "" : rawResponse;
  }

  public String rawResponse() {
    return rawResponse;
  }

  public boolean empty() {
    return rawResponse.isEmpty();
  }
}
