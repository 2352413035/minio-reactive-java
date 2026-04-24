package io.minio.reactive.messages;

/** S3 SelectObjectContent 请求模型。 */
public final class SelectObjectContentRequest {
  private final String expression;
  private final String expressionType;
  private final String inputSerializationXml;
  private final String outputSerializationXml;
  private final boolean requestProgress;

  public SelectObjectContentRequest(
      String expression,
      String expressionType,
      String inputSerializationXml,
      String outputSerializationXml,
      boolean requestProgress) {
    if (expression == null || expression.trim().isEmpty()) {
      throw new IllegalArgumentException("S3 Select 表达式不能为空");
    }
    this.expression = expression.trim();
    this.expressionType =
        expressionType == null || expressionType.trim().isEmpty() ? "SQL" : expressionType.trim();
    this.inputSerializationXml =
        inputSerializationXml == null || inputSerializationXml.trim().isEmpty()
            ? "<CSV><FileHeaderInfo>USE</FileHeaderInfo></CSV><CompressionType>NONE</CompressionType>"
            : inputSerializationXml.trim();
    this.outputSerializationXml =
        outputSerializationXml == null || outputSerializationXml.trim().isEmpty()
            ? "<CSV><RecordDelimiter>\n</RecordDelimiter></CSV>"
            : outputSerializationXml.trim();
    this.requestProgress = requestProgress;
  }

  public static SelectObjectContentRequest csv(String expression) {
    return new SelectObjectContentRequest(expression, "SQL", null, null, false);
  }

  public String expression() {
    return expression;
  }

  public String expressionType() {
    return expressionType;
  }

  public String inputSerializationXml() {
    return inputSerializationXml;
  }

  public String outputSerializationXml() {
    return outputSerializationXml;
  }

  public boolean requestProgress() {
    return requestProgress;
  }
}
