package io.minio.reactive.examples;

import io.minio.reactive.ReactiveMinioClient;
import reactor.core.publisher.Mono;

public class TestCreateBucket {
    public static void main(String[] args) {
        ReactiveMinioClient client =
                ReactiveMinioClient.builder()
//                        .region("niuniuniujia")
                        .endpoint("http://127.0.0.1:9000")
                        .credentials("nFpKQzMpRQlBn05PrjzT", "Sac0vCSvF3gEuDHUqulAXfLltuaXu1OGl09ILdJO")
                        .build();
        Mono<String> then = client.makeBucket("test-bucket2").doOnSuccess((ignore)-> System.out.println("bucket created"))
                .then(
                        client.getBucketLocation("test-bucket2").doOnNext(System.out::println)
                );

        then.block();

    }
}
