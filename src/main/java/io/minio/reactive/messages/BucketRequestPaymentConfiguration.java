package io.minio.reactive.messages;

/** bucket request payment 配置摘要。 */
public final class BucketRequestPaymentConfiguration {
  private final String payer;
  private final String rawXml;

  public BucketRequestPaymentConfiguration(String payer, String rawXml) {
    this.payer = payer == null ? "" : payer;
    this.rawXml = rawXml == null ? "" : rawXml;
  }

  public String payer() {
    return payer;
  }

  public String rawXml() {
    return rawXml;
  }

  public boolean requesterPays() {
    return "Requester".equalsIgnoreCase(payer);
  }
}
