package io.minio.reactive.messages;

/** Part descriptor used when completing multipart upload. */
public final class CompletePart {
  private final int partNumber;
  private final String etag;

  public CompletePart(int partNumber, String etag) {
    if (partNumber < 1) {
      throw new IllegalArgumentException("partNumber must be positive");
    }
    if (etag == null || etag.trim().isEmpty()) {
      throw new IllegalArgumentException("etag must not be empty");
    }
    this.partNumber = partNumber;
    this.etag = etag;
  }

  public int partNumber() {
    return partNumber;
  }

  public String etag() {
    return etag;
  }
}
