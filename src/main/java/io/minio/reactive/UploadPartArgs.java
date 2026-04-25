package io.minio.reactive;

/** 上传 multipart part 的参数对象。 */
public final class UploadPartArgs extends ObjectArgs {
  private final String uploadId;
  private final int partNumber;
  private final byte[] content;

  private UploadPartArgs(Builder builder) {
    super(builder);
    this.uploadId = requireText("uploadId", builder.uploadId);
    this.partNumber = (int) requirePositive("partNumber", builder.partNumber);
    this.content = builder.content == null ? new byte[0] : builder.content.clone();
  }
  public String uploadId() { return uploadId; }
  public int partNumber() { return partNumber; }
  public byte[] content() { return content.clone(); }
  public static Builder builder() { return new Builder(); }
  public static final class Builder extends ObjectArgs.AbstractBuilder<Builder> {
    private String uploadId; private int partNumber; private byte[] content;
    public Builder uploadId(String uploadId) { this.uploadId = uploadId; return this; }
    public Builder partNumber(int partNumber) { this.partNumber = partNumber; return this; }
    public Builder content(byte[] content) { this.content = content == null ? new byte[0] : content.clone(); return this; }
    @Override protected Builder self() { return this; }
    public UploadPartArgs build() { return new UploadPartArgs(this); }
  }
}
