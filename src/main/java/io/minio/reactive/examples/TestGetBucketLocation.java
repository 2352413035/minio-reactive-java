package io.minio.reactive.examples;

import io.minio.reactive.ReactiveMinioClient;
import reactor.core.publisher.Mono;

import java.io.InputStream;
import java.util.Properties;
import java.util.UUID;

public class TestGetBucketLocation {
    public static void main(String[] args) {

        ReactiveMinioClient client =
                ReactiveMinioClient.builder()
                        .endpoint("http://127.0.0.1:9000")
                        .credentials("nFpKQzMpRQlBn05PrjzT", "Sac0vCSvF3gEuDHUqulAXfLltuaXu1OGl09ILdJO")
//                        .region("niuniuniujia")
                        .build();

        Mono<String> bucketLocation = client.getBucketLocation("ai-project-bucket");
        bucketLocation.doOnNext(System.out::println).block();
    }


}
