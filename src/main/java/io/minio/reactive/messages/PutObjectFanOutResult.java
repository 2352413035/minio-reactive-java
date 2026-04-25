package io.minio.reactive.messages;

/** FanOut 单个目标对象的写入结果。 */
public final class PutObjectFanOutResult {
  private final String key;
  private final String etag;
  private final String versionId;
  private final String error;

  public PutObjectFanOutResult(String key, String etag, String versionId, String error) {
    this.key = key == null ? "" : key;
    this.etag = etag == null ? "" : etag;
    this.versionId = versionId == null ? "" : versionId;
    this.error = error == null ? "" : error;
  }

  public String key() {
    return key;
  }

  public String etag() {
    return etag;
  }

  public String versionId() {
    return versionId;
  }

  public String error() {
    return error;
  }

  public boolean success() {
    return error.isEmpty();
  }
}
