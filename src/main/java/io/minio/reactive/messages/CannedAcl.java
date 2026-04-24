package io.minio.reactive.messages;

/** S3 canned ACL 请求头取值。 */
public enum CannedAcl {
  PRIVATE("private"),
  PUBLIC_READ("public-read"),
  PUBLIC_READ_WRITE("public-read-write"),
  AUTHENTICATED_READ("authenticated-read"),
  BUCKET_OWNER_READ("bucket-owner-read"),
  BUCKET_OWNER_FULL_CONTROL("bucket-owner-full-control");

  private final String headerValue;

  CannedAcl(String headerValue) {
    this.headerValue = headerValue;
  }

  public String headerValue() {
    return headerValue;
  }
}
