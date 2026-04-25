package io.minio.reactive;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Args 请求对象的公共基类。
 *
 * <p>它只保存 SDK 迁移层需要的通用校验工具，不承载 HTTP 细节；真正的请求签名、XML、
 * header 仍由 `ReactiveMinioClient` 的强类型方法负责。
 */
public class BaseArgs {
  protected BaseArgs() {}

  protected static String requireText(String name, String value) {
    if (value == null || value.trim().isEmpty()) {
      throw new IllegalArgumentException(name + " 不能为空");
    }
    return value;
  }

  protected static long requireNonNegative(String name, long value) {
    if (value < 0L) {
      throw new IllegalArgumentException(name + " 不能小于 0");
    }
    return value;
  }

  protected static long requirePositive(String name, long value) {
    if (value <= 0L) {
      throw new IllegalArgumentException(name + " 必须大于 0");
    }
    return value;
  }

  protected static <T> List<T> copyList(Iterable<T> values, String name) {
    if (values == null) {
      throw new IllegalArgumentException(name + " 不能为空");
    }
    List<T> result = new ArrayList<T>();
    for (T value : values) {
      if (value == null) {
        throw new IllegalArgumentException(name + " 不能包含空元素");
      }
      result.add(value);
    }
    if (result.isEmpty()) {
      throw new IllegalArgumentException(name + " 不能为空");
    }
    return Collections.unmodifiableList(result);
  }

  protected static Map<String, String> copyStringMap(Map<String, String> values, String name) {
    if (values == null || values.isEmpty()) {
      throw new IllegalArgumentException(name + " 不能为空");
    }
    Map<String, String> result = new LinkedHashMap<String, String>();
    for (Map.Entry<String, String> entry : values.entrySet()) {
      if (entry.getKey() == null || entry.getKey().trim().isEmpty()) {
        throw new IllegalArgumentException(name + " 不能包含空 key");
      }
      if (entry.getValue() == null) {
        throw new IllegalArgumentException(name + " 不能包含空 value");
      }
      result.put(entry.getKey(), entry.getValue());
    }
    return Collections.unmodifiableMap(result);
  }
}
