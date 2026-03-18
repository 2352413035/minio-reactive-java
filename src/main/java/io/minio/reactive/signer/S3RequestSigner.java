package io.minio.reactive.signer;

import io.minio.reactive.ReactiveMinioClientConfig;
import io.minio.reactive.credentials.ReactiveCredentials;
import io.minio.reactive.http.S3Request;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
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
 * Minimal AWS SigV4 signer for standard S3 requests.
 *
 * <p>This covers the core S3 signing flow: canonical request, string to sign,
 * derived signing key, and Authorization header.
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
    builder.header("Host", config.endpointUri().getHost() + portSuffix(config));
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
      // Anonymous access keeps the normalized request headers but skips Authorization.
      return builder.build();
    }

    S3Request unsignedRequest = builder.build();
    Map<String, String> canonicalHeaders = canonicalHeaders(unsignedRequest.headers());
    String signedHeaders = signedHeaders(canonicalHeaders);
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
      // Avoid signing unstable headers that may be rewritten by the HTTP client stack.
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
