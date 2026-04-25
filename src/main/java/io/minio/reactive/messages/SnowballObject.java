package io.minio.reactive.messages;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.ZonedDateTime;

/** Snowball 批量上传中的单个对象。 */
public final class SnowballObject {
  private final String name;
  private final byte[] content;
  private final Path filename;
  private final ZonedDateTime modificationTime;

  private SnowballObject(String name, byte[] content, Path filename, ZonedDateTime modificationTime) {
    this.name = normalizeName(name);
    this.content = content;
    this.filename = filename;
    this.modificationTime = modificationTime;
    if (content == null && filename == null) {
      throw new IllegalArgumentException("SnowballObject 必须提供 content 或 filename");
    }
  }

  public static SnowballObject of(String name, byte[] content) {
    return new SnowballObject(name, content == null ? new byte[0] : content, null, null);
  }

  public static SnowballObject ofString(String name, String content) {
    return of(name, (content == null ? "" : content).getBytes(StandardCharsets.UTF_8));
  }

  public static SnowballObject ofFile(String name, Path filename) {
    if (filename == null) {
      throw new IllegalArgumentException("filename 不能为空");
    }
    return new SnowballObject(name, null, filename, null);
  }

  public SnowballObject withModificationTime(ZonedDateTime modificationTime) {
    return new SnowballObject(name, content, filename, modificationTime);
  }

  public String name() {
    return name;
  }

  public byte[] content() {
    return content;
  }

  public Path filename() {
    return filename;
  }

  public ZonedDateTime modificationTime() {
    return modificationTime;
  }

  private static String normalizeName(String name) {
    if (name == null || name.trim().isEmpty()) {
      throw new IllegalArgumentException("snowball object name 不能为空");
    }
    return name.startsWith("/") ? name.substring(1) : name;
  }
}
