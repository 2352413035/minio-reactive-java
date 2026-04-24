package io.minio.reactive.messages.admin;

import java.util.Arrays;

/**
 * Admin 诊断二进制响应包装。
 *
 * <p>inspect-data 和 profiling download 可能返回压缩包或二进制诊断文件。这里保留字节数组、来源和大小，
 * 不把二进制内容误解码为字符串，避免破坏诊断文件。
 */
public final class AdminBinaryResult {
  private final String source;
  private final byte[] bytes;

  public AdminBinaryResult(String source, byte[] bytes) {
    this.source = source == null ? "" : source;
    this.bytes = bytes == null ? new byte[0] : Arrays.copyOf(bytes, bytes.length);
  }

  public static AdminBinaryResult of(String source, byte[] bytes) {
    return new AdminBinaryResult(source, bytes);
  }

  public String source() {
    return source;
  }

  public byte[] bytes() {
    return Arrays.copyOf(bytes, bytes.length);
  }

  public int size() {
    return bytes.length;
  }

  public boolean isEmpty() {
    return bytes.length == 0;
  }
}
