package io.minio.reactive.messages;

/** bucket 静态网站配置摘要。 */
public final class BucketWebsiteConfiguration {
  private final String indexDocumentSuffix;
  private final String errorDocumentKey;
  private final String rawXml;

  public BucketWebsiteConfiguration(String indexDocumentSuffix, String errorDocumentKey, String rawXml) {
    this.indexDocumentSuffix = indexDocumentSuffix == null ? "" : indexDocumentSuffix;
    this.errorDocumentKey = errorDocumentKey == null ? "" : errorDocumentKey;
    this.rawXml = rawXml == null ? "" : rawXml;
  }

  public String indexDocumentSuffix() {
    return indexDocumentSuffix;
  }

  public String errorDocumentKey() {
    return errorDocumentKey;
  }

  public String rawXml() {
    return rawXml;
  }
}
