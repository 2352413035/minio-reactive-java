package io.minio.reactive;

import io.minio.reactive.messages.ComposeSource;

/** multipart copy part 的参数对象。 */
public final class UploadPartCopyArgs extends ObjectArgs {
  private final String uploadId;
  private final int partNumber;
  private final ComposeSource source;

  private UploadPartCopyArgs(Builder builder) {
    super(builder);
    this.uploadId = requireText("uploadId", builder.uploadId);
    this.partNumber = (int) requirePositive("partNumber", builder.partNumber);
    if (builder.source == null) { throw new IllegalArgumentException("source 不能为空"); }
    this.source = builder.source;
  }
  public String uploadId() { return uploadId; }
  public int partNumber() { return partNumber; }
  public ComposeSource source() { return source; }
  public static Builder builder() { return new Builder(); }
  public static final class Builder extends ObjectArgs.AbstractBuilder<Builder> {
    private String uploadId; private int partNumber; private ComposeSource source;
    public Builder uploadId(String uploadId) { this.uploadId = uploadId; return this; }
    public Builder partNumber(int partNumber) { this.partNumber = partNumber; return this; }
    public Builder source(ComposeSource source) { this.source = source; return this; }
    @Override protected Builder self() { return this; }
    public UploadPartCopyArgs build() { return new UploadPartCopyArgs(this); }
  }
}
