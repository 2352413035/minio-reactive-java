package io.minio.reactive.messages.admin;

/**
 * Admin 诊断文本响应包装。
 *
 * <p>这类接口通常返回纯文本、pprof 文本或版本相关的诊断内容。SDK 只固定来源和原文，
 * 不猜测服务端每个版本的文本格式，调用方可以按自己的排障流程解析 rawText()。
 */
public final class AdminTextResult {
  private final String source;
  private final String rawText;

  public AdminTextResult(String source, String rawText) {
    this.source = source == null ? "" : source;
    this.rawText = rawText == null ? "" : rawText;
  }

  public static AdminTextResult of(String source, String rawText) {
    return new AdminTextResult(source, rawText);
  }

  public String source() {
    return source;
  }

  public String rawText() {
    return rawText;
  }

  public boolean isEmpty() {
    return rawText.trim().isEmpty();
  }
}
