package io.minio.reactive.http;

import io.minio.reactive.ReactiveMinioClientConfig;
import io.minio.reactive.errors.ReactiveS3Exception;
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
 * Reactive HTTP sender for S3-compatible requests.
 *
 * <p>Important implementation note: response bodies must be consumed inside
 * {@code exchangeToMono}/{@code exchangeToFlux}. An earlier version returned
 * {@code ClientResponse} first and tried to decode the body later, which made successful GET
 * requests look like empty content because the body had already been released.
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

  public Mono<byte[]> exchangeToByteArray(S3Request request) {
    return requestSpec(request)
        .exchangeToMono(
            response -> {
              if (!response.statusCode().is2xxSuccessful()) {
                return readError(response).flatMap(Mono::error);
              }
              return response.bodyToMono(byte[].class).defaultIfEmpty(new byte[0]);
            });
  }

  private WebClient.RequestHeadersSpec<?> requestSpec(S3Request request) {
    WebClient.RequestBodySpec bodySpec =
        webClient
            .method(request.method())
            .uri(request.toUri(config))
            .headers(headers -> apply(headers, request));

    if (request.hasBody()) {
      // PUT bucket and PUT object both depend on explicit body + content type here.
      return bodySpec.contentType(request.contentType()).body(BodyInserters.fromValue(request.body()));
    }

    return bodySpec;
  }

  private static Mono<ReactiveS3Exception> readError(ClientResponse response) {
    return response
        .bodyToMono(String.class)
        .defaultIfEmpty("")
        .map(body -> new ReactiveS3Exception(response.statusCode().value(), body));
  }

  private static void apply(HttpHeaders headers, S3Request request) {
    request.headers().forEach(headers::add);
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
