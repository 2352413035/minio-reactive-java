package io.minio.reactive.examples;

import io.minio.reactive.ReactiveMinioClient;
import reactor.core.publisher.Mono;

public class TestGetBucketLocation {
    public static void main(String[] args) {

        ReactiveMinioClient client =
                ReactiveMinioClient.builder()
                        .endpoint("http://127.0.0.1:9000")
                        .credentials("your-access-key", "your-secret-key")
//                        .region("niuniuniujia")
                        .build();

        Mono<String> bucketLocation = client.getBucketLocation("ai-project-bucket");
        bucketLocation.doOnNext(System.out::println).block();
    }


}
