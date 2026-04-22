package io.minio.reactive.util;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.StringJoiner;

/**
 * S3 规范化请求所需的最小编码工具。
 *
 * <p>SigV4 并不是对“原始字符串”直接签名，而是对规范化后的 URI 和 query 进行签名。
 * 因此这里的编码规则必须和签名器、实际请求 URI 保持一致。
 */
public final class S3Escaper {
  private static final String UNRESERVED =
      "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789-_.~";

  private S3Escaper() {}

  public static String encodePathSegment(String value) {
    return encode(value, false);
  }

  public static String encodeQueryComponent(String value) {
    return encode(value, true);
  }

  public static String canonicalQueryString(Map<String, String> queryParameters) {
    if (queryParameters == null || queryParameters.isEmpty()) {
      return "";
    }

    List<String> pairs = new ArrayList<String>();
    for (Map.Entry<String, String> entry : queryParameters.entrySet()) {
      String key = encodeQueryComponent(entry.getKey());
      String value = encodeQueryComponent(entry.getValue() == null ? "" : entry.getValue());
      pairs.add(key + "=" + value);
    }
    Collections.sort(pairs);

    StringJoiner joiner = new StringJoiner("&");
    for (String pair : pairs) {
      joiner.add(pair);
    }
    return joiner.toString();
  }

  public static String canonicalPath(String path) {
    if (path == null || path.trim().isEmpty() || "/".equals(path.trim())) {
      return "/";
    }
    String value = path.trim();
    if (!value.startsWith("/")) {
      value = "/" + value;
    }
    String[] tokens = value.substring(1).split("/", -1);
    StringBuilder builder = new StringBuilder("/");
    for (int i = 0; i < tokens.length; i++) {
      if (i > 0) {
        builder.append('/');
      }
      builder.append(encodePathSegment(tokens[i]));
    }
    return builder.toString();
  }

  public static String canonicalUri(String bucket, String object) {
    StringBuilder builder = new StringBuilder("/");
    if (bucket != null && !bucket.trim().isEmpty()) {
      builder.append(encodePathSegment(bucket));
    }
    if (object != null && !object.trim().isEmpty()) {
      if (builder.charAt(builder.length() - 1) != '/') {
        builder.append('/');
      }
      String[] tokens = object.split("/", -1);
      for (int i = 0; i < tokens.length; i++) {
        if (i > 0) {
          builder.append('/');
        }
        builder.append(encodePathSegment(tokens[i]));
      }
    }
    return builder.toString();
  }

  private static String encode(String value, boolean encodeSlash) {
    if (value == null) {
      return "";
    }

    StringBuilder builder = new StringBuilder();
    byte[] bytes = value.getBytes(StandardCharsets.UTF_8);
    for (byte current : bytes) {
      int unsigned = current & 0xFF;
      char ch = (char) unsigned;
      if (UNRESERVED.indexOf(ch) >= 0 || (!encodeSlash && ch == '/')) {
        builder.append(ch);
      } else {
        builder.append('%');
        char high = Character.toUpperCase(Character.forDigit((unsigned >> 4) & 0xF, 16));
        char low = Character.toUpperCase(Character.forDigit(unsigned & 0xF, 16));
        builder.append(high).append(low);
      }
    }
    return builder.toString();
  }
}
