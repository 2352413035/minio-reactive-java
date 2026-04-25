package io.minio.reactive.credentials;

import com.fasterxml.jackson.databind.JsonNode;
import io.minio.reactive.util.JsonSupport;
import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.ProviderException;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

/** provider 体系内部工具；所有错误都使用中文，且不输出 secret。 */
final class CredentialProviderSupport {
  private CredentialProviderSupport() {}

  static String requireText(String name, String value) {
    if (value == null || value.trim().isEmpty()) {
      throw new IllegalArgumentException(name + " 不能为空");
    }
    return value;
  }

  static String optionalText(String value) {
    return value == null || value.trim().isEmpty() ? null : value;
  }

  static Credentials credentials(String accessKey, String secretKey, String sessionToken) {
    return new Credentials(requireText("accessKey", accessKey), requireText("secretKey", secretKey), optionalText(sessionToken));
  }

  static Credentials readAwsProfile(Path file, String profile) {
    Map<String, Map<String, String>> profiles = readIni(file);
    Map<String, String> values = profiles.get(profile);
    if (values == null) {
      throw new ProviderException("AWS 凭证文件中不存在 profile: " + profile);
    }
    return credentials(values.get("aws_access_key_id"), values.get("aws_secret_access_key"), values.get("aws_session_token"));
  }

  static Map<String, Map<String, String>> readIni(Path file) {
    Map<String, Map<String, String>> result = new LinkedHashMap<String, Map<String, String>>();
    try (BufferedReader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
      String section = null;
      String line;
      while ((line = reader.readLine()) != null) {
        String trimmed = line.trim();
        if (trimmed.isEmpty() || trimmed.startsWith("#") || trimmed.startsWith(";")) {
          continue;
        }
        if (trimmed.startsWith("[") && trimmed.endsWith("]")) {
          section = trimmed.substring(1, trimmed.length() - 1).trim();
          result.put(section, new LinkedHashMap<String, String>());
          continue;
        }
        int index = trimmed.indexOf('=');
        if (index < 0) {
          index = trimmed.indexOf(':');
        }
        if (index > 0 && section != null) {
          String key = trimmed.substring(0, index).trim();
          String value = trimmed.substring(index + 1).trim();
          result.get(section).put(key, value);
        }
      }
    } catch (IOException e) {
      throw new ProviderException("无法读取凭证配置文件: " + file, e);
    }
    return result;
  }

  static Credentials readMinioConfig(Path file, String alias) {
    try {
      String json = new String(Files.readAllBytes(file), StandardCharsets.UTF_8);
      JsonNode root = JsonSupport.parseTree(json);
      JsonNode hosts = JsonSupport.child(root, "hosts", "Hosts");
      JsonNode target = hosts == null ? null : hosts.get(alias);
      if (target == null || target.isNull()) {
        throw new ProviderException("MinIO 客户端配置中不存在 alias: " + alias);
      }
      return credentials(
          JsonSupport.textAny(target, "accessKey", "access_key"),
          JsonSupport.textAny(target, "secretKey", "secret_key"),
          JsonSupport.textAny(target, "sessionToken", "session_token"));
    } catch (IOException e) {
      throw new ProviderException("无法读取 MinIO 客户端配置文件: " + file, e);
    }
  }

  static Path defaultMinioConfigPath() {
    String home = System.getProperty("user.home");
    String osName = System.getProperty("os.name", "").toLowerCase(Locale.US);
    return java.nio.file.Paths.get(home, osName.contains("windows") ? "mc" : ".mc", "config.json");
  }
}
