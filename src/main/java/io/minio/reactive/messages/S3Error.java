package io.minio.reactive.messages;

/** Structured S3 XML error payload. */
public final class S3Error {
  private final String code;
  private final String message;
  private final String bucketName;
  private final String key;
  private final String requestId;
  private final String hostId;
  private final String resource;

  public S3Error(
      String code,
      String message,
      String bucketName,
      String key,
      String requestId,
      String hostId,
      String resource) {
    this.code = code;
    this.message = message;
    this.bucketName = bucketName;
    this.key = key;
    this.requestId = requestId;
    this.hostId = hostId;
    this.resource = resource;
  }

  public String code() {
    return code;
  }

  public String message() {
    return message;
  }

  public String bucketName() {
    return bucketName;
  }

  public String key() {
    return key;
  }

  public String requestId() {
    return requestId;
  }

  public String hostId() {
    return hostId;
  }

  public String resource() {
    return resource;
  }
}
