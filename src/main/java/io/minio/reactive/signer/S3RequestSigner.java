package io.minio.reactive.signer;

import io.minio.reactive.ReactiveMinioClientConfig;
import io.minio.reactive.credentials.ReactiveCredentials;
import io.minio.reactive.http.S3Request;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

/**
 * 最小可用的 AWS SigV4 签名器。
 *
 * <p>当前实现已经覆盖标准 S3 请求所需的核心步骤：
 *
 * <ol>
 *   <li>生成 payload 的 SHA-256 摘要
 *   <li>构造 canonical request
 *   <li>构造 string to sign
 *   <li>推导 signing key
 *   <li>生成 Authorization 头
 * </ol>
 *
 * <p>这部分是整个 SDK 最协议化的代码，读懂它对理解 MinIO/S3 请求为什么这样发很关键。
 */
public final class S3RequestSigner {
  private static final DateTimeFormatter AMZ_DATE =
      DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss'Z'");
  private static final DateTimeFormatter SHORT_DATE = DateTimeFormatter.ofPattern("yyyyMMdd");

  public S3Request sign(
      S3Request request, ReactiveMinioClientConfig config, ReactiveCredentials credentials) {
    ZonedDateTime now = ZonedDateTime.now(ZoneOffset.UTC);
    String amzDate = now.format(AMZ_DATE);
    String shortDate = now.format(SHORT_DATE);
    String payloadHash = sha256Hex(request.hasBody() ? request.body() : new byte[0]);

    S3Request.Builder builder = request.toBuilder();
    builder.header("Host", hostHeader(config));
    builder.header("User-Agent", "minio-reactive-java/0.1.0-SNAPSHOT");
    builder.header("X-Amz-Date", amzDate);
    builder.header("X-Amz-Content-Sha256", payloadHash);
    if (request.headers().containsKey("Content-Length")) {
      builder.header("Content-Length", request.headers().get("Content-Length"));
    }
    if (request.headers().containsKey("Content-Type")) {
      builder.header("Content-Type", request.headers().get("Content-Type"));
    }
    if (credentials.sessionToken() != null && !credentials.sessionToken().trim().isEmpty()) {
      builder.header("X-Amz-Security-Token", credentials.sessionToken());
    }

    if (credentials.isAnonymous()) {
      // 匿名访问不生成 Authorization，但仍然保留前面补充的标准头部。
      return builder.build();
    }

    S3Request unsignedRequest = builder.build();
    Map<String, String> canonicalHeaders = canonicalHeaders(unsignedRequest.headers());
    String signedHeaders = signedHeaders(canonicalHeaders);

    // canonical request 是服务端验签时会按同样规则重建的文本，因此格式必须严格一致。
    String canonicalRequest =
        unsignedRequest.method().name()
            + "\n"
            + unsignedRequest.canonicalUri()
            + "\n"
            + unsignedRequest.canonicalQueryString()
            + "\n"
            + canonicalHeadersString(canonicalHeaders)
            + "\n"
            + signedHeaders
            + "\n"
            + payloadHash;

    String scope = shortDate + "/" + config.region() + "/s3/aws4_request";
    String stringToSign =
        "AWS4-HMAC-SHA256\n"
            + amzDate
            + "\n"
            + scope
            + "\n"
            + sha256Hex(canonicalRequest.getBytes(StandardCharsets.UTF_8));

    byte[] signingKey = signingKey(credentials.secretKey(), shortDate, config.region(), "s3");
    String signature = hex(hmac(signingKey, stringToSign));
    String authorization =
        "AWS4-HMAC-SHA256 Credential="
            + credentials.accessKey()
            + "/"
            + scope
            + ", SignedHeaders="
            + signedHeaders
            + ", Signature="
            + signature;

    return unsignedRequest.toBuilder().header("Authorization", authorization).build();
  }


  public URI presign(
      S3Request request,
      ReactiveMinioClientConfig config,
      ReactiveCredentials credentials,
      Duration expires) {
    if (credentials.isAnonymous()) {
      return request.toUri(config);
    }
    long expiresSeconds = expires == null ? 900L : expires.getSeconds();
    if (expiresSeconds < 1L || expiresSeconds > 604800L) {
      throw new IllegalArgumentException(
          "presigned URL expiry must be between 1 and 604800 seconds");
    }

    ZonedDateTime now = ZonedDateTime.now(ZoneOffset.UTC);
    String amzDate = now.format(AMZ_DATE);
    String shortDate = now.format(SHORT_DATE);
    String scope = shortDate + "/" + config.region() + "/s3/aws4_request";
    String signedHeaders = "host";

    S3Request.Builder builder = request.toBuilder();
    builder.header("Host", hostHeader(config));
    builder.queryParameter("X-Amz-Algorithm", "AWS4-HMAC-SHA256");
    builder.queryParameter("X-Amz-Credential", credentials.accessKey() + "/" + scope);
    builder.queryParameter("X-Amz-Date", amzDate);
    builder.queryParameter("X-Amz-Expires", Long.toString(expiresSeconds));
    builder.queryParameter("X-Amz-SignedHeaders", signedHeaders);
    if (credentials.sessionToken() != null && !credentials.sessionToken().trim().isEmpty()) {
      builder.queryParameter("X-Amz-Security-Token", credentials.sessionToken());
    }

    S3Request unsignedRequest = builder.build();
    Map<String, String> canonicalHeaders = new LinkedHashMap<String, String>();
    canonicalHeaders.put("host", hostHeader(config));

    String canonicalRequest =
        unsignedRequest.method().name()
            + "\n"
            + unsignedRequest.canonicalUri()
            + "\n"
            + unsignedRequest.canonicalQueryString()
            + "\n"
            + canonicalHeadersString(canonicalHeaders)
            + "\n"
            + signedHeaders
            + "\n"
            + "UNSIGNED-PAYLOAD";

    String stringToSign =
        "AWS4-HMAC-SHA256\n"
            + amzDate
            + "\n"
            + scope
            + "\n"
            + sha256Hex(canonicalRequest.getBytes(StandardCharsets.UTF_8));

    byte[] signingKey = signingKey(credentials.secretKey(), shortDate, config.region(), "s3");
    String signature = hex(hmac(signingKey, stringToSign));
    S3Request presigned =
        unsignedRequest.toBuilder().queryParameter("X-Amz-Signature", signature).build();
    return presigned.toUri(config);
  }

  private static String hostHeader(ReactiveMinioClientConfig config) {
    return config.endpointUri().getHost() + portSuffix(config);
  }

  private static String portSuffix(ReactiveMinioClientConfig config) {
    int port = config.endpointUri().getPort();
    if (port < 0) {
      return "";
    }
    boolean defaultHttp = "http".equalsIgnoreCase(config.endpointUri().getScheme()) && port == 80;
    boolean defaultHttps =
        "https".equalsIgnoreCase(config.endpointUri().getScheme()) && port == 443;
    return defaultHttp || defaultHttps ? "" : ":" + port;
  }

  private static Map<String, String> canonicalHeaders(Map<String, String> headers) {
    List<String> names = new ArrayList<String>(headers.keySet());
    Collections.sort(names, String.CASE_INSENSITIVE_ORDER);
    Map<String, String> canonical = new LinkedHashMap<String, String>();
    for (String name : names) {
      String signedName = name.toLowerCase(Locale.US);
      // 这些头可能被底层 HTTP 客户端改写或补充，不适合作为稳定签名输入。
      if ("authorization".equals(signedName)
          || "user-agent".equals(signedName)
          || "accept-encoding".equals(signedName)) {
        continue;
      }
      canonical.put(signedName, normalizeHeaderValue(headers.get(name)));
    }
    return canonical;
  }

  private static String canonicalHeadersString(Map<String, String> headers) {
    StringBuilder builder = new StringBuilder();
    for (Map.Entry<String, String> entry : headers.entrySet()) {
      builder.append(entry.getKey()).append(':').append(entry.getValue()).append('\n');
    }
    return builder.toString();
  }

  private static String signedHeaders(Map<String, String> headers) {
    StringBuilder builder = new StringBuilder();
    int index = 0;
    for (String name : headers.keySet()) {
      if (index++ > 0) {
        builder.append(';');
      }
      builder.append(name);
    }
    return builder.toString();
  }

  private static String normalizeHeaderValue(String value) {
    return value == null ? "" : value.trim().replaceAll("\\s+", " ");
  }

  private static byte[] signingKey(String secretKey, String date, String region, String service) {
    byte[] kDate = hmac(("AWS4" + secretKey).getBytes(StandardCharsets.UTF_8), date);
    byte[] kRegion = hmac(kDate, region);
    byte[] kService = hmac(kRegion, service);
    return hmac(kService, "aws4_request");
  }

  private static byte[] hmac(byte[] key, String data) {
    try {
      Mac mac = Mac.getInstance("HmacSHA256");
      mac.init(new SecretKeySpec(key, "HmacSHA256"));
      return mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
    } catch (Exception e) {
      throw new IllegalStateException("Unable to calculate HMAC-SHA256", e);
    }
  }

  private static String sha256Hex(byte[] input) {
    try {
      MessageDigest digest = MessageDigest.getInstance("SHA-256");
      return hex(digest.digest(input == null ? new byte[0] : input));
    } catch (NoSuchAlgorithmException e) {
      throw new IllegalStateException("SHA-256 not available", e);
    }
  }

  private static String hex(byte[] bytes) {
    StringBuilder builder = new StringBuilder(bytes.length * 2);
    for (byte current : bytes) {
      int value = current & 0xFF;
      if (value < 16) {
        builder.append('0');
      }
      builder.append(Integer.toHexString(value));
    }
    return builder.toString();
  }
}
