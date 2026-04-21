# 09 MinIO API Catalog Coverage

This project now has two API layers:

1. `ReactiveMinioClient` — typed, Reactor-friendly helpers for the common S3 object-storage SDK surface.
2. `ReactiveMinioRawClient` + `MinioApiCatalog` — a route-complete, protocol-level executor for every MinIO server HTTP interface discovered in the local `minio` router files.

## Covered router sources

The catalog mirrors these local MinIO files:

- `minio/cmd/api-router.go`
- `minio/cmd/admin-router.go`
- `minio/cmd/kms-router.go`
- `minio/cmd/sts-handlers.go`
- `minio/cmd/healthcheck-router.go`
- `minio/cmd/metrics-router.go`

## Catalog families

Current catalog size: 233 endpoints.

| Family | Count | Source |
| --- | ---: | --- |
| `s3` | 77 | S3 object, bucket, root routes |
| `admin` | 128 | MinIO admin `/minio/admin/v3` routes |
| `kms` | 7 | MinIO KMS `/minio/kms/v1` routes |
| `health` | 8 | `/minio/health/*` readiness/liveness routes |
| `metrics` | 6 | Prometheus and metrics v2/v3 routes |
| `sts` | 7 | AWS STS-compatible routes |

## Usage

```java
ReactiveMinioClient client =
    ReactiveMinioClient.builder()
        .endpoint("http://127.0.0.1:9000")
        .region("us-east-1")
        .credentials(accessKey, secretKey)
        .build();

ReactiveMinioRawClient raw = client.rawClient();

String xml = raw.executeToString(
    MinioApiCatalog.byName("S3_LIST_BUCKETS"),
    Collections.<String, String>emptyMap(),
    Collections.<String, String>emptyMap(),
    Collections.<String, String>emptyMap(),
    null,
    null).block();
```

Use the typed client for stable object-storage flows. Use the raw client for admin, KMS, STS, metrics, health, or newly-added MinIO routes before a typed model exists.

## Design notes

- Catalog endpoints include method, path template, auth requirement, fixed query parameters, and required dynamic query parameters.
- `ReactiveMinioRawClient` expands path variables, merges fixed and dynamic query values, signs auth-required calls with the same SigV4 signer, and delegates to the shared WebFlux HTTP layer.
- Health endpoints are marked with `authScheme=none`.
- Metrics endpoints use `authScheme=bearer` because default MinIO metrics auth expects a bearer/JWT token unless `MINIO_PROMETHEUS_AUTH_TYPE=public` is configured. The raw client therefore permits caller-supplied `Authorization: Bearer ...` only for bearer endpoints and does not SigV4-sign them.
- S3/Admin/KMS and signed STS endpoints use `authScheme=sigv4`; STS signed form requests use SigV4 service scope `sts`, while S3/Admin/KMS use `s3`.
- Admin/KMS/STS typed request/response classes are intentionally deferred; their wire models are broad and version-sensitive. The route-complete raw layer prevents missing public interfaces while typed models can be added incrementally.

## Scope boundary

`MinioApiCatalog` covers public/client-facing HTTP interfaces registered by the MinIO server router. Distributed erasure internals registered from `routers.go` (`storage`, `peer`, `bootstrap`, namespace lock, and grid routes) are private node-to-node protocols, not stable public SDK APIs. They are intentionally excluded from this Java SDK surface unless a future task explicitly targets MinIO internal cluster protocol clients.
