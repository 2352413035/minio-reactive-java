package io.minio.reactive.util;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.StringJoiner;

/**
 * Minimal percent-encoding helpers for S3 canonical requests.
 */
public final class S3Escaper {
  private static final String UNRESERVED = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789-_.~";

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
