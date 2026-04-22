package io.minio.reactive.http;

import io.minio.reactive.ReactiveMinioClientConfig;
import io.minio.reactive.errors.ReactiveS3Exception;
import java.io.ByteArrayOutputStream;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.http.HttpHeaders;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * S3 兼容请求的响应式 HTTP 发送层。
 *
 * <p>这里封装的是“怎么发请求”和“怎么读取响应”。
 * 当前实现中一个非常关键的经验是：响应体必须在
 * {@code exchangeToMono}/{@code exchangeToFlux} 的回调内部消费，
 * 不能先把 {@code ClientResponse} 取出来再到外面读 body，
 * 否则会出现请求成功但响应体为空的情况。
 */
public final class ReactiveHttpClient {
  private final WebClient webClient;
  @SuppressWarnings("unused")
  private final ReactiveMinioClientConfig config;

  public ReactiveHttpClient(WebClient webClient, ReactiveMinioClientConfig config) {
    this.webClient = webClient;
    this.config = config;
  }

  public Mono<Integer> exchangeToStatus(S3Request request) {
    return requestSpec(request)
        .exchangeToMono(
            response -> {
              if (!response.statusCode().is2xxSuccessful()) {
                return readError(response).flatMap(Mono::error);
              }
              return response.releaseBody().thenReturn(response.statusCode().value());
            });
  }

  public Mono<Map<String, List<String>>> exchangeToHeaders(S3Request request) {
    return requestSpec(request)
        .exchangeToMono(
            response -> {
              if (!response.statusCode().is2xxSuccessful()) {
                return readError(response).flatMap(Mono::error);
              }
              Map<String, List<String>> headers =
                  response.headers().asHttpHeaders().entrySet().stream()
                      .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
              return response.releaseBody().thenReturn(headers);
            });
  }

  public Mono<Void> exchangeToVoid(S3Request request) {
    return requestSpec(request)
        .exchangeToMono(
            response -> {
              if (!response.statusCode().is2xxSuccessful()) {
                return readError(response).flatMap(Mono::error);
              }
              return response.releaseBody();
            });
  }

  public Flux<byte[]> exchangeToBody(S3Request request) {
    return requestSpec(request)
        .exchangeToFlux(
            response -> {
              if (!response.statusCode().is2xxSuccessful()) {
                return readError(response).flatMapMany(Flux::error);
              }
              return response.bodyToFlux(DataBuffer.class).map(ReactiveHttpClient::toBytes);
            });
  }

  public Mono<String> exchangeToString(S3Request request) {
    return exchangeToByteArray(request)
        .map(bytes -> new String(bytes, java.nio.charset.StandardCharsets.UTF_8));
  }

  public Mono<byte[]> exchangeToByteArray(S3Request request) {
    return requestSpec(request)
        .exchangeToMono(
            response -> {
              if (!response.statusCode().is2xxSuccessful()) {
                return readError(response).flatMap(Mono::error);
              }
              return response
                  .bodyToFlux(DataBuffer.class)
                  .reduce(new ByteArrayOutputStream(), ReactiveHttpClient::append)
                  .map(ByteArrayOutputStream::toByteArray)
                  .defaultIfEmpty(new byte[0]);
            });
  }

  private WebClient.RequestHeadersSpec<?> requestSpec(S3Request request) {
    WebClient.RequestBodySpec bodySpec =
        webClient
            .method(request.method())
            .uri(request.toUri(config))
            .headers(headers -> apply(headers, request));

    if (request.hasBody()) {
      // 创建桶和上传对象都依赖这里把请求体和 Content-Type 正确写出去。
      return bodySpec
          .contentType(request.contentType())
          .body(BodyInserters.fromValue(request.body()));
    }

    return bodySpec;
  }

  private static Mono<ReactiveS3Exception> readError(ClientResponse response) {
    return response
        .bodyToMono(String.class)
        .defaultIfEmpty("")
        .map(body -> new ReactiveS3Exception(
            response.statusCode().value(), body, response.headers().asHttpHeaders().getFirst("x-amz-request-id")));
  }

  private static void apply(HttpHeaders headers, S3Request request) {
    request.headers().forEach(headers::add);
  }

  private static ByteArrayOutputStream append(ByteArrayOutputStream output, DataBuffer dataBuffer) {
    try {
      byte[] bytes = new byte[dataBuffer.readableByteCount()];
      dataBuffer.read(bytes);
      output.write(bytes, 0, bytes.length);
      return output;
    } finally {
      DataBufferUtils.release(dataBuffer);
    }
  }

  private static byte[] toBytes(DataBuffer dataBuffer) {
    try {
      byte[] bytes = new byte[dataBuffer.readableByteCount()];
      dataBuffer.read(bytes);
      return bytes;
    } finally {
      DataBufferUtils.release(dataBuffer);
    }
  }
}
