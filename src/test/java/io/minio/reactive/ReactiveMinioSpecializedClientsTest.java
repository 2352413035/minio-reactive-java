package io.minio.reactive;

import io.minio.reactive.catalog.MinioApiCatalog;
import io.minio.reactive.messages.admin.AddServiceAccountRequest;
import io.minio.reactive.messages.admin.AdminAccountSummary;
import io.minio.reactive.messages.admin.AdminBatchJobList;
import io.minio.reactive.messages.admin.AdminBucketQuota;
import io.minio.reactive.messages.admin.AdminConfigHelp;
import io.minio.reactive.messages.admin.AdminDataUsageSummary;
import io.minio.reactive.messages.admin.AdminStorageSummary;
import io.minio.reactive.messages.admin.AdminIdpConfigList;
import io.minio.reactive.messages.admin.AdminPolicyEntities;
import io.minio.reactive.messages.admin.AdminRemoteTargetList;
import io.minio.reactive.messages.admin.AdminTierList;
import io.minio.reactive.messages.admin.EncryptedAdminResponse;
import io.minio.reactive.messages.admin.UpdateGroupMembersRequest;
import io.minio.reactive.messages.admin.AddUserRequest;
import io.minio.reactive.messages.BucketCorsConfiguration;
import io.minio.reactive.messages.BucketCorsRule;
import io.minio.reactive.messages.BucketNotificationConfiguration;
import io.minio.reactive.messages.BucketNotificationTarget;
import io.minio.reactive.messages.BucketReplicationMetrics;
import io.minio.reactive.messages.CannedAcl;
import io.minio.reactive.messages.ObjectLegalHoldConfiguration;
import io.minio.reactive.messages.ObjectRetentionConfiguration;
import io.minio.reactive.messages.RestoreObjectRequest;
import io.minio.reactive.messages.SelectObjectContentRequest;
import io.minio.reactive.messages.sts.AssumeRoleResult;
import io.minio.reactive.messages.sts.AssumeRoleSsoRequest;
import io.minio.reactive.messages.sts.AssumeRoleWithCertificateRequest;
import io.minio.reactive.messages.sts.AssumeRoleWithCustomTokenRequest;
import io.minio.reactive.util.MadminEncryptionSupport;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

class ReactiveMinioSpecializedClientsTest {
  @Test
  void shouldExposePeerClientBuilders() {
    Assertions.assertNotNull(
        ReactiveMinioClient.builder().endpoint("http://localhost:9000").region("us-east-1").build());
    Assertions.assertNotNull(
        ReactiveMinioAdminClient.builder().endpoint("http://localhost:9000").region("us-east-1").build());
    Assertions.assertNotNull(
        ReactiveMinioKmsClient.builder().endpoint("http://localhost:9000").region("us-east-1").build());
    Assertions.assertNotNull(
        ReactiveMinioStsClient.builder().endpoint("http://localhost:9000").region("us-east-1").build());
    Assertions.assertNotNull(
        ReactiveMinioMetricsClient.builder().endpoint("http://localhost:9000").region("us-east-1").build());
    Assertions.assertNotNull(
        ReactiveMinioHealthClient.builder().endpoint("http://localhost:9000").region("us-east-1").build());
    Assertions.assertNotNull(
        ReactiveMinioRawClient.builder().endpoint("http://localhost:9000").region("us-east-1").build());
  }

  @Test
  void shouldExposeRepresentativeCatalogMethodsOnSpecializedClients() {
    assertMonoMethodExists(ReactiveMinioClient.class, "s3ListBuckets");
    assertMonoMethodExists(ReactiveMinioClient.class, "s3GetObject");
    assertMonoMethodExists(ReactiveMinioAdminClient.class, "serverInfo");
    assertMonoMethodExists(ReactiveMinioAdminClient.class, "addUser");
    assertMonoMethodExists(ReactiveMinioKmsClient.class, "keyStatus");
    assertMonoMethodExists(ReactiveMinioStsClient.class, "assumeRoleWithWebIdentity");
    assertMonoMethodExists(ReactiveMinioMetricsClient.class, "v3");
    assertMonoMethodExists(ReactiveMinioHealthClient.class, "liveGet");
  }

  @Test
  void shouldKeepSpecializedMethodsAlignedWithCatalogFamilies() {
    Assertions.assertTrue(countDistinctMonoMethods(ReactiveMinioClient.class, "s3") >= 77);
    Assertions.assertTrue(
        countDistinctMonoMethods(ReactiveMinioAdminClient.class, null)
            >= MinioApiCatalog.byFamily("admin").size());
    Assertions.assertTrue(
        countDistinctMonoMethods(ReactiveMinioKmsClient.class, null)
            >= MinioApiCatalog.byFamily("kms").size());
    Assertions.assertTrue(
        countDistinctMonoMethods(ReactiveMinioStsClient.class, null)
            >= MinioApiCatalog.byFamily("sts").size());
    Assertions.assertTrue(
        countDistinctMonoMethods(ReactiveMinioMetricsClient.class, null)
            >= MinioApiCatalog.byFamily("metrics").size());
    Assertions.assertTrue(
        countDistinctMonoMethods(ReactiveMinioHealthClient.class, null)
            >= MinioApiCatalog.byFamily("health").size());
  }


  @Test
  void shouldKeepAdvancedCompatibilityBaselineForMigration() {
    assertAdvancedBaseline(ReactiveMinioClient.class, 129, 60, 5);
    assertAdvancedBaseline(ReactiveMinioAdminClient.class, 201, 7, 0);
    assertAdvancedBaseline(ReactiveMinioKmsClient.class, 8, 0, 0);
    assertAdvancedBaseline(ReactiveMinioStsClient.class, 14, 6, 0);
    assertAdvancedBaseline(ReactiveMinioMetricsClient.class, 6, 0, 0);
    assertAdvancedBaseline(ReactiveMinioHealthClient.class, 0, 0, 0);
    assertAdvancedBaseline(ReactiveMinioRawClient.class, 3, 0, 8);
  }


  @Test
  void shouldNotExposeEndpointExecutorInPublicApi() {
    Class<?>[] publicTypes = {
      ReactiveMinioClient.class,
      ReactiveMinioAdminClient.class,
      ReactiveMinioKmsClient.class,
      ReactiveMinioStsClient.class,
      ReactiveMinioMetricsClient.class,
      ReactiveMinioHealthClient.class,
      ReactiveMinioRawClient.class
    };
    for (Class<?> type : publicTypes) {
      for (Method method : type.getMethods()) {
        Assertions.assertNotEquals(ReactiveMinioEndpointExecutor.class, method.getReturnType());
        for (Class<?> parameterType : method.getParameterTypes()) {
          Assertions.assertNotEquals(ReactiveMinioEndpointExecutor.class, parameterType);
        }
      }
    }
  }


  @Test
  void shouldTreatHealthBusinessMethodsAsBooleanResults() {
    WebClient webClient =
        WebClient.builder()
            .exchangeFunction(
                request ->
                    Mono.just(
                        ClientResponse.create(HttpStatus.SERVICE_UNAVAILABLE)
                            .body("not ready")
                            .build()))
            .build();
    ReactiveMinioHealthClient client =
        ReactiveMinioHealthClient.builder()
            .endpoint("http://localhost:9000")
            .region("us-east-1")
            .webClient(webClient)
            .build();

    Assertions.assertEquals(Boolean.FALSE, client.isReady().block());
    Assertions.assertEquals(503, client.checkReadiness().block().statusCode());
  }


  @Test
  void shouldBuildAdminIdentityBusinessRequests() {
    java.util.List<String> paths = new java.util.ArrayList<String>();
    java.util.List<String> queries = new java.util.ArrayList<String>();
    WebClient webClient =
        WebClient.builder()
            .exchangeFunction(
                request -> {
                  paths.add(request.url().getPath());
                  queries.add(request.url().getQuery());
                  return Mono.just(ClientResponse.create(HttpStatus.OK).body("{}").build());
                })
            .build();
    ReactiveMinioAdminClient admin =
        ReactiveMinioAdminClient.builder()
            .endpoint("http://localhost:9000")
            .region("us-east-1")
            .webClient(webClient)
            .credentials("ak", "sk")
            .build();

    admin.setGroupEnabled("dev", false).block();
    admin.updateGroupMembers(UpdateGroupMembersRequest.add("dev", java.util.Collections.singletonList("user1"))).block();
    admin.deleteServiceAccountTyped("svc1").block();

    Assertions.assertTrue(paths.contains("/minio/admin/v3/set-group-status"));
    Assertions.assertTrue(paths.contains("/minio/admin/v3/update-group-members"));
    Assertions.assertTrue(paths.contains("/minio/admin/v3/delete-service-account"));
    Assertions.assertTrue(containsAllQueryParts(queries, "group=dev", "status=disabled"));
    Assertions.assertTrue(containsAllQueryParts(queries, "accessKey=svc1"));
  }


  @Test
  void shouldBuildPolicyAndKmsBusinessRequests() {
    java.util.List<String> paths = new java.util.ArrayList<String>();
    java.util.List<String> queries = new java.util.ArrayList<String>();
    WebClient webClient =
        WebClient.builder()
            .exchangeFunction(
                request -> {
                  paths.add(request.url().getPath());
                  queries.add(request.url().getQuery());
                  return Mono.just(ClientResponse.create(HttpStatus.OK).body("{}").build());
                })
            .build();
    ReactiveMinioAdminClient admin =
        ReactiveMinioAdminClient.builder()
            .endpoint("http://localhost:9000")
            .region("us-east-1")
            .webClient(webClient)
            .credentials("ak", "sk")
            .build();
    ReactiveMinioKmsClient kms =
        ReactiveMinioKmsClient.builder()
            .endpoint("http://localhost:9000")
            .region("us-east-1")
            .webClient(webClient)
            .credentials("ak", "sk")
            .build();
    ReactiveMinioMetricsClient metrics =
        ReactiveMinioMetricsClient.builder()
            .endpoint("http://localhost:9000")
            .region("us-east-1")
            .webClient(webClient)
            .credentials("ak", "sk")
            .build();

    admin.putPolicy("readonly", "{\"Version\":\"2012-10-17\"}").block();
    admin.setUserPolicy("readonly", "user1").block();
    kms.getKeyStatus("key1").block();
    Assertions.assertEquals("kms", kms.scrapeMetrics().block().scope());
    Assertions.assertEquals("legacy", metrics.scrapeLegacyMetrics("token").block().scope());

    Assertions.assertTrue(paths.contains("/minio/admin/v3/add-canned-policy"));
    Assertions.assertTrue(paths.contains("/minio/kms/v1/metrics"));
    Assertions.assertTrue(paths.contains("/minio/prometheus/metrics"));
    Assertions.assertTrue(queries.contains("name=readonly"));
    Assertions.assertTrue(containsAllQueryParts(queries, "policyName=readonly", "userOrGroup=user1", "isGroup=false"));
    Assertions.assertTrue(queries.contains("key-id=key1"));
  }


  @Test
  void shouldExposeStage23KmsAndMetricsTypedMethods() {
    assertMonoMethodExists(ReactiveMinioKmsClient.class, "scrapeMetrics");
    assertMonoMethodExists(ReactiveMinioMetricsClient.class, "scrapeLegacyMetrics");
  }

  @Test
  void shouldExposeStage29StsAdvancedIdentityMethods() {
    assertMonoMethodExists(ReactiveMinioStsClient.class, "assumeRoleSsoCredentials");
    assertMonoMethodExists(ReactiveMinioStsClient.class, "assumeRoleWithCertificateCredentials");
    assertMonoMethodExists(ReactiveMinioStsClient.class, "assumeRoleWithCustomTokenCredentials");
    assertAllPublicOverloadsDeprecated(ReactiveMinioStsClient.class, "assumeRoleSsoForm");
    assertAllPublicOverloadsDeprecated(ReactiveMinioStsClient.class, "assumeRoleWithCertificate");
    assertAllPublicOverloadsDeprecated(ReactiveMinioStsClient.class, "assumeRoleWithCustomToken");
  }

  @Test
  void shouldBuildStage29StsAdvancedIdentityRequests() {
    java.util.List<String> queries = new java.util.ArrayList<String>();
    WebClient webClient =
        WebClient.builder()
            .exchangeFunction(
                request -> {
                  queries.add(request.url().getQuery());
                  return Mono.just(ClientResponse.create(HttpStatus.OK).body(stsSuccessXml()).build());
                })
            .build();
    ReactiveMinioStsClient sts =
        ReactiveMinioStsClient.builder()
            .endpoint("http://localhost:9000")
            .region("us-east-1")
            .webClient(webClient)
            .build();

    AssumeRoleResult sso =
        sts.assumeRoleSsoCredentials(
                AssumeRoleSsoRequest.webIdentity("jwt").withRoleArn("arn:minio:iam:::role/demo"))
            .block();
    AssumeRoleResult certificate =
        sts.assumeRoleWithCertificateCredentials(
                AssumeRoleWithCertificateRequest.create().withDurationSeconds(900))
            .block();
    AssumeRoleResult custom =
        sts.assumeRoleWithCustomTokenCredentials(
                AssumeRoleWithCustomTokenRequest.of("opaque").withRoleArn("arn:minio:iam:::role/demo"))
            .block();

    Assertions.assertEquals("sts-access", sso.credentials().accessKey());
    Assertions.assertEquals("sts-access", certificate.credentials().accessKey());
    Assertions.assertEquals("sts-access", custom.credentials().accessKey());
    Assertions.assertTrue(containsAllQueryParts(queries, "Action=AssumeRoleWithCertificate", "DurationSeconds=900"));
    Assertions.assertTrue(containsAllQueryParts(queries, "Action=AssumeRoleWithCustomToken", "Token=opaque"));
    Assertions.assertThrows(IllegalArgumentException.class, () -> AssumeRoleSsoRequest.webIdentity(""));
    Assertions.assertThrows(IllegalArgumentException.class, () -> AssumeRoleWithCustomTokenRequest.of(""));
  }



  @Test
  void shouldMarkMigratedS3CatalogMethodsDeprecated() {
    assertDeprecatedMethodExists(ReactiveMinioClient.class, "s3HeadObject");
    assertDeprecatedMethodExists(ReactiveMinioClient.class, "s3GetObject");
    assertDeprecatedMethodExists(ReactiveMinioClient.class, "s3PutObject");
    assertDeprecatedMethodExists(ReactiveMinioClient.class, "s3DeleteObject");
    assertDeprecatedMethodExists(ReactiveMinioClient.class, "s3ListObjectsV2");
    assertDeprecatedMethodExists(ReactiveMinioClient.class, "s3CreateMultipartUpload");
    assertDeprecatedMethodExists(ReactiveMinioClient.class, "s3PutObjectPart");
    assertDeprecatedMethodExists(ReactiveMinioClient.class, "s3CompleteMultipartUpload");
    assertDeprecatedMethodExists(ReactiveMinioClient.class, "s3AbortMultipartUpload");
  }



  @Test
  void shouldEncryptAddUserRequestPayload() {
    byte[] encrypted =
        MadminEncryptionSupport.encryptData(
            "admin-secret",
            io.minio.reactive.util.JsonSupport.toJsonBytes(
                AddUserRequest.of("user1", "user-secret").toPayload()));

    Assertions.assertTrue(MadminEncryptionSupport.isEncrypted(encrypted));
    Assertions.assertTrue(
        new String(
                MadminEncryptionSupport.decryptData("admin-secret", encrypted),
                java.nio.charset.StandardCharsets.UTF_8)
            .contains("user-secret"));
  }


  @Test
  void shouldBuildEncryptedServiceAccountRequestPayload() {
    AddServiceAccountRequest request =
        AddServiceAccountRequest.builder()
            .name("svc1")
            .description("demo service account")
            .policyJson("{\"Version\":\"2012-10-17\"}")
            .build();
    byte[] encrypted =
        MadminEncryptionSupport.encryptData(
            "admin-secret", io.minio.reactive.util.JsonSupport.toJsonBytes(request.toPayload()));
    String decrypted =
        new String(
            MadminEncryptionSupport.decryptData("admin-secret", encrypted),
            java.nio.charset.StandardCharsets.UTF_8);

    Assertions.assertTrue(MadminEncryptionSupport.isEncrypted(encrypted));
    Assertions.assertTrue(decrypted.contains("svc1"));
    Assertions.assertTrue(decrypted.contains("demo service account"));
  }


  @Test
  void shouldExposeServiceAccountBusinessMethod() {
    assertMonoMethodExists(ReactiveMinioAdminClient.class, "addServiceAccount");
    EncryptedAdminResponse response = new EncryptedAdminResponse(new byte[41]);
    Assertions.assertTrue(response.isEncrypted());
  }


  @Test
  void shouldExposeConfigWriteBusinessMethods() {
    assertMonoMethodExists(ReactiveMinioAdminClient.class, "setConfigKvText");
    assertMonoMethodExists(ReactiveMinioAdminClient.class, "setConfigText");
    ReactiveMinioAdminClient admin =
        ReactiveMinioAdminClient.builder()
            .endpoint("http://localhost:9000")
            .region("us-east-1")
            .credentials("ak", "sk")
            .build();

    Assertions.assertThrows(IllegalArgumentException.class, () -> admin.setConfigKvText(" "));
    Assertions.assertThrows(IllegalArgumentException.class, () -> admin.setConfigText(""));
  }



  @Test
  void shouldExposeAccessKeyBusinessMethods() {
    assertMonoMethodExists(ReactiveMinioAdminClient.class, "getAccessKeyInfoEncrypted");
    assertMonoMethodExists(ReactiveMinioAdminClient.class, "listAccessKeysEncrypted");
    assertMonoMethodExists(ReactiveMinioAdminClient.class, "getAccessKeyInfoTyped");
    assertMonoMethodExists(ReactiveMinioAdminClient.class, "listAccessKeysTyped");
  }


  @Test
  void shouldParseStage15AdminPlaintextSafeModels() {
    AdminConfigHelp help =
        AdminConfigHelp.parse(
            "{\"subSys\":\"api\",\"description\":\"API 配置\",\"multipleTargets\":false,\"keysHelp\":[{\"key\":\"requests_max\",\"description\":\"最大请求数\",\"optional\":true,\"type\":\"number\",\"multipleTargets\":false}]}");
    Assertions.assertEquals("api", help.subSys());
    Assertions.assertEquals("requests_max", help.keys().get(0));
    Assertions.assertTrue(help.keysHelp().get(0).optional());

    AdminStorageSummary storage =
        AdminStorageSummary.parse(
            "{\"Disks\":[{\"healing\":true},{\"healing\":false}],\"Backend\":{\"Type\":1,\"OnlineDisks\":{\"pool-0\":2},\"OfflineDisks\":{\"pool-0\":1}}}");
    Assertions.assertEquals(2, storage.diskCount());
    Assertions.assertEquals(2, storage.onlineDiskCount());
    Assertions.assertEquals(1, storage.offlineDiskCount());
    Assertions.assertEquals(1, storage.healingDiskCount());

    AdminDataUsageSummary usage =
        AdminDataUsageSummary.parse(
            "{\"objectsCount\":7,\"objectsTotalSize\":4096,\"bucketsCount\":3,\"capacity\":100,\"freeCapacity\":60,\"usedCapacity\":40}");
    Assertions.assertEquals(7, usage.objectsCount());
    Assertions.assertEquals(4096, usage.objectsTotalSize());
    Assertions.assertEquals(3, usage.bucketsCount());
    Assertions.assertEquals(40, usage.totalUsedCapacity());

    AdminAccountSummary account =
        AdminAccountSummary.parse(
            "{\"AccountName\":\"root\",\"Server\":{\"Type\":1},\"Policy\":{\"Version\":\"2012-10-17\"},\"Buckets\":[{\"name\":\"a\",\"access\":{\"read\":true,\"write\":true}},{\"name\":\"b\",\"access\":{\"read\":true,\"write\":false}}]}");
    Assertions.assertEquals("root", account.accountName());
    Assertions.assertEquals(2, account.bucketCount());
    Assertions.assertEquals(2, account.readableBucketCount());
    Assertions.assertEquals(1, account.writableBucketCount());
    Assertions.assertTrue(account.policyJson().contains("2012-10-17"));

    AdminBucketQuota quota =
        AdminBucketQuota.parse("{\"quota\":1024,\"size\":2048,\"rate\":0,\"requests\":0,\"quotatype\":\"hard\"}");
    Assertions.assertEquals(1024, quota.quota());
    Assertions.assertEquals(2048, quota.size());
    Assertions.assertEquals("hard", quota.quotaType());

    AdminTierList tiers =
        AdminTierList.parse(
            "[{\"Version\":\"v1\",\"Type\":\"s3\",\"Name\":\"archive\"},{\"Version\":\"v1\",\"Type\":\"minio\",\"Name\":\"backup\"}]");
    Assertions.assertEquals(2, tiers.tierCount());
    Assertions.assertEquals("archive", tiers.tiers().get(0).name());

    AdminPolicyEntities entities =
        AdminPolicyEntities.parse(
            "{\"UserMappings\":{\"user1\":[\"readonly\"]},\"GroupMappings\":{\"dev\":[\"readwrite\"]},\"PolicyMappings\":{\"readonly\":[\"user1\"]}}");
    Assertions.assertEquals(3, entities.totalMappingCount());

    AdminIdpConfigList idps = AdminIdpConfigList.parse("openid", "{\"primary\":{}}");
    Assertions.assertEquals("openid", idps.type());
    Assertions.assertEquals("primary", idps.names().get(0));

    AdminRemoteTargetList targets =
        AdminRemoteTargetList.parse(
            "{\"Targets\":[{\"Arn\":\"arn:minio:replication::bucket:target\",\"Type\":\"replication\",\"Endpoint\":\"http://remote\",\"Secure\":true}]}");
    Assertions.assertEquals(1, targets.targetCount());
    Assertions.assertTrue(targets.targets().get(0).secure());

    AdminBatchJobList jobs =
        AdminBatchJobList.parse(
            "{\"Jobs\":[{\"ID\":\"job-1\",\"Type\":\"replicate\",\"Status\":\"running\",\"User\":\"root\"}]}");
    Assertions.assertEquals(1, jobs.jobCount());
    Assertions.assertEquals("running", jobs.jobs().get(0).status());
  }


  @Test
  void shouldBuildStage15AdminPlaintextSafeRequests() {
    java.util.List<String> paths = new java.util.ArrayList<String>();
    java.util.List<String> queries = new java.util.ArrayList<String>();
    WebClient webClient =
        WebClient.builder()
            .exchangeFunction(
                request -> {
                  paths.add(request.url().getPath());
                  queries.add(request.url().getQuery());
                  return Mono.just(
                      ClientResponse.create(HttpStatus.OK)
                          .body(stage15AdminResponseBody(request.url().getPath()))
                          .build());
                })
            .build();
    ReactiveMinioAdminClient admin =
        ReactiveMinioAdminClient.builder()
            .endpoint("http://localhost:9000")
            .region("us-east-1")
            .webClient(webClient)
            .credentials("ak", "sk")
            .build();

    admin.getConfigHelp("api", "requests_max").block();
    Assertions.assertEquals(3, admin.listPolicyEntities().block().totalMappingCount());
    Assertions.assertEquals("primary", admin.listIdpConfigs("openid").block().names().get(0));
    Assertions.assertEquals("ok", admin.getIdpConfigInfo("openid", "primary").block().values().get("status"));
    admin.getStorageSummary().block();
    admin.getDataUsageSummary().block();
    admin.getAccountSummary().block();
    Assertions.assertTrue(admin.getAccessKeyInfoEncrypted("svc1").block().encryptedData().length > 0);
    Assertions.assertTrue(admin.listAccessKeysEncrypted("all").block().encryptedData().length > 0);
    admin.getBucketQuotaInfo("bucket1").block();
    admin.listTiers().block();
    Assertions.assertEquals(1, admin.listRemoteTargetsInfo("bucket1", "replication").block().targetCount());
    Assertions.assertEquals(1, admin.listBatchJobsInfo().block().jobCount());
    Assertions.assertEquals("running", admin.getBatchJobStatusInfo().block().values().get("status"));
    Assertions.assertEquals("job-1", admin.describeBatchJobInfo().block().values().get("id"));
    admin.getBackgroundHealStatus().block();
    admin.listPoolsInfo().block();
    admin.getPoolStatus("pool-0").block();
    admin.getRebalanceStatus().block();
    admin.getTierStats().block();
    admin.getSiteReplicationInfo().block();
    admin.getSiteReplicationStatus().block();
    Assertions.assertEquals("ok", admin.getSiteReplicationMetainfo().block().values().get("status"));
    admin.getTopLocksInfo().block();
    admin.getObdInfo().block();
    admin.getHealthInfo().block();
    Assertions.assertEquals(
        "trace-line",
        new String(admin.traceStream().blockFirst(), java.nio.charset.StandardCharsets.UTF_8).trim());
    Assertions.assertEquals(
        "log-line",
        new String(admin.logStream().blockFirst(), java.nio.charset.StandardCharsets.UTF_8).trim());
    Assertions.assertTrue(admin.getConfigKvEncrypted("api").block().encryptedData().length > 0);
    Assertions.assertTrue(admin.listConfigHistoryKvEncrypted(1).block().encryptedData().length > 0);
    Assertions.assertTrue(admin.getConfigEncrypted().block().encryptedData().length > 0);
    Assertions.assertThrows(UnsupportedOperationException.class, () -> admin.getAccessKeyInfoTyped("svc1").block());
    Assertions.assertThrows(UnsupportedOperationException.class, () -> admin.listAccessKeysTyped("all").block());

    Assertions.assertTrue(paths.contains("/minio/admin/v3/help-config-kv"));
    Assertions.assertTrue(paths.contains("/minio/admin/v3/idp/builtin/policy-entities"));
    Assertions.assertTrue(paths.contains("/minio/admin/v3/idp-config/openid"));
    Assertions.assertTrue(paths.contains("/minio/admin/v3/idp-config/openid/primary"));
    Assertions.assertTrue(paths.contains("/minio/admin/v3/storageinfo"));
    Assertions.assertTrue(paths.contains("/minio/admin/v3/datausageinfo"));
    Assertions.assertTrue(paths.contains("/minio/admin/v3/accountinfo"));
    Assertions.assertTrue(paths.contains("/minio/admin/v3/info-access-key"));
    Assertions.assertTrue(paths.contains("/minio/admin/v3/list-access-keys-bulk"));
    Assertions.assertTrue(paths.contains("/minio/admin/v3/get-bucket-quota"));
    Assertions.assertTrue(paths.contains("/minio/admin/v3/tier"));
    Assertions.assertTrue(paths.contains("/minio/admin/v3/list-remote-targets"));
    Assertions.assertTrue(paths.contains("/minio/admin/v3/list-jobs"));
    Assertions.assertTrue(paths.contains("/minio/admin/v3/status-job"));
    Assertions.assertTrue(paths.contains("/minio/admin/v3/describe-job"));
    Assertions.assertTrue(paths.contains("/minio/admin/v3/background-heal/status"));
    Assertions.assertTrue(paths.contains("/minio/admin/v3/pools/list"));
    Assertions.assertTrue(paths.contains("/minio/admin/v3/pools/status"));
    Assertions.assertTrue(paths.contains("/minio/admin/v3/rebalance/status"));
    Assertions.assertTrue(paths.contains("/minio/admin/v3/tier-stats"));
    Assertions.assertTrue(paths.contains("/minio/admin/v3/site-replication/info"));
    Assertions.assertTrue(paths.contains("/minio/admin/v3/site-replication/metainfo"));
    Assertions.assertTrue(paths.contains("/minio/admin/v3/site-replication/status"));
    Assertions.assertTrue(paths.contains("/minio/admin/v3/top/locks"));
    Assertions.assertTrue(paths.contains("/minio/admin/v3/obdinfo"));
    Assertions.assertTrue(paths.contains("/minio/admin/v3/healthinfo"));
    Assertions.assertTrue(paths.contains("/minio/admin/v3/trace"));
    Assertions.assertTrue(paths.contains("/minio/admin/v3/log"));
    Assertions.assertTrue(paths.contains("/minio/admin/v3/get-config-kv"));
    Assertions.assertTrue(paths.contains("/minio/admin/v3/list-config-history-kv"));
    Assertions.assertTrue(paths.contains("/minio/admin/v3/config"));
    Assertions.assertTrue(containsAllQueryParts(queries, "subSys=api", "key=requests_max"));
    Assertions.assertTrue(containsAllQueryParts(queries, "accessKey=svc1"));
    Assertions.assertTrue(containsAllQueryParts(queries, "listType=all"));
    Assertions.assertTrue(containsAllQueryParts(queries, "bucket=bucket1"));
    Assertions.assertTrue(containsAllQueryParts(queries, "bucket=bucket1", "type=replication"));
    Assertions.assertTrue(containsAllQueryParts(queries, "pool=pool-0"));
    Assertions.assertTrue(containsAllQueryParts(queries, "key=api"));
    Assertions.assertThrows(IllegalArgumentException.class, () -> admin.listConfigHistoryKvEncrypted(-1));
  }


  @Test
  void shouldExposeStage22AdminJsonSummaryMethods() {
    assertMonoMethodExists(ReactiveMinioAdminClient.class, "getBackgroundHealStatus");
    assertMonoMethodExists(ReactiveMinioAdminClient.class, "listPoolsInfo");
    assertMonoMethodExists(ReactiveMinioAdminClient.class, "getPoolStatus");
    assertMonoMethodExists(ReactiveMinioAdminClient.class, "getRebalanceStatus");
    assertMonoMethodExists(ReactiveMinioAdminClient.class, "getTierStats");
    assertMonoMethodExists(ReactiveMinioAdminClient.class, "getSiteReplicationInfo");
    assertMonoMethodExists(ReactiveMinioAdminClient.class, "getSiteReplicationStatus");
    assertMonoMethodExists(ReactiveMinioAdminClient.class, "getSiteReplicationMetainfo");
    assertMonoMethodExists(ReactiveMinioAdminClient.class, "getTopLocksInfo");
    assertMonoMethodExists(ReactiveMinioAdminClient.class, "getObdInfo");
    assertMonoMethodExists(ReactiveMinioAdminClient.class, "getHealthInfo");
    assertFluxMethodExists(ReactiveMinioAdminClient.class, "traceStream");
    assertFluxMethodExists(ReactiveMinioAdminClient.class, "logStream");
    ReactiveMinioAdminClient admin =
        ReactiveMinioAdminClient.builder().endpoint("http://localhost:9000").region("us-east-1").build();
    Assertions.assertThrows(IllegalArgumentException.class, () -> admin.getPoolStatus(" "));
  }

  @Test
  void shouldExposeStage30AdminSummaryMethods() {
    assertMonoMethodExists(ReactiveMinioAdminClient.class, "listPolicyEntities");
    assertMonoMethodExists(ReactiveMinioAdminClient.class, "listIdpConfigs");
    assertMonoMethodExists(ReactiveMinioAdminClient.class, "getIdpConfigInfo");
    assertMonoMethodExists(ReactiveMinioAdminClient.class, "listRemoteTargetsInfo");
    assertMonoMethodExists(ReactiveMinioAdminClient.class, "listBatchJobsInfo");
    assertMonoMethodExists(ReactiveMinioAdminClient.class, "getBatchJobStatusInfo");
    assertMonoMethodExists(ReactiveMinioAdminClient.class, "describeBatchJobInfo");
    assertDeprecatedMethodExists(ReactiveMinioAdminClient.class, "listBuiltinPolicyEntities");
    assertDeprecatedMethodExists(ReactiveMinioAdminClient.class, "listIdpConfig");
    assertDeprecatedMethodExists(ReactiveMinioAdminClient.class, "getIdpConfig");
    assertDeprecatedMethodExists(ReactiveMinioAdminClient.class, "listRemoteTargets");
    assertDeprecatedMethodExists(ReactiveMinioAdminClient.class, "listBatchJobs");
    assertDeprecatedMethodExists(ReactiveMinioAdminClient.class, "batchJobStatus");
    assertDeprecatedMethodExists(ReactiveMinioAdminClient.class, "describeBatchJob");
  }


  @Test
  void shouldExposeS3VersioningBusinessMethods() {
    assertMonoMethodExists(ReactiveMinioClient.class, "getBucketVersioningConfiguration");
    assertMonoMethodExists(ReactiveMinioClient.class, "setBucketVersioningConfiguration");
    assertMonoMethodExists(ReactiveMinioClient.class, "listObjectVersionsPage");
    assertMonoMethodExists(ReactiveMinioClient.class, "listMultipartUploadsPage");
    assertFluxMethodExists(ReactiveMinioClient.class, "listObjectVersions");
    assertFluxMethodExists(ReactiveMinioClient.class, "listMultipartUploads");
    assertMonoMethodExists(ReactiveMinioStsClient.class, "assumeRoleCredentials");
    assertDeprecatedMethodExists(ReactiveMinioClient.class, "s3GetBucketVersioning");
    assertDeprecatedMethodExists(ReactiveMinioClient.class, "s3PutBucketVersioning");
    assertDeprecatedMethodExists(ReactiveMinioClient.class, "s3GetObjectTagging");
    assertAllPublicOverloadsDeprecated(ReactiveMinioClient.class, "s3PutObjectTagging");
    assertAllPublicOverloadsDeprecated(ReactiveMinioClient.class, "s3DeleteObjectTagging");
    assertAllPublicOverloadsDeprecated(ReactiveMinioClient.class, "s3PutBucketTagging");
    assertAllPublicOverloadsDeprecated(ReactiveMinioClient.class, "s3PutBucketVersioning");
  }


  @Test
  void shouldExposeS3ObjectGovernanceBusinessMethods() {
    assertMonoMethodExists(ReactiveMinioClient.class, "getObjectAttributes");
    assertMonoMethodExists(ReactiveMinioClient.class, "getObjectRetention");
    assertMonoMethodExists(ReactiveMinioClient.class, "setObjectRetention");
    assertMonoMethodExists(ReactiveMinioClient.class, "getObjectLegalHold");
    assertMonoMethodExists(ReactiveMinioClient.class, "setObjectLegalHold");
    assertMonoMethodExists(ReactiveMinioClient.class, "restoreObject");
    assertDeprecatedMethodExists(ReactiveMinioClient.class, "s3GetObjectAttributes");
    assertDeprecatedMethodExists(ReactiveMinioClient.class, "s3GetObjectRetention");
    assertDeprecatedMethodExists(ReactiveMinioClient.class, "s3GetObjectLegalHold");
    assertAllPublicOverloadsDeprecated(ReactiveMinioClient.class, "s3PutObjectRetention");
    assertAllPublicOverloadsDeprecated(ReactiveMinioClient.class, "s3PutObjectLegalHold");
    assertAllPublicOverloadsDeprecated(ReactiveMinioClient.class, "s3PostRestoreObject");
  }


  @Test
  void shouldBuildS3ObjectGovernanceRequests() {
    java.util.List<String> paths = new java.util.ArrayList<String>();
    java.util.List<String> queries = new java.util.ArrayList<String>();
    java.util.List<String> objectAttributeHeaders = new java.util.ArrayList<String>();
    WebClient webClient =
        WebClient.builder()
            .exchangeFunction(
                request -> {
                  paths.add(request.url().getPath());
                  queries.add(request.url().getQuery());
                  objectAttributeHeaders.add(request.headers().getFirst("X-Amz-Object-Attributes"));
                  return Mono.just(
                      ClientResponse.create(HttpStatus.OK)
                          .body(stage20S3ResponseBody(request.url().getQuery()))
                          .build());
                })
            .build();
    ReactiveMinioClient client =
        ReactiveMinioClient.builder()
            .endpoint("http://localhost:9000")
            .region("us-east-1")
            .webClient(webClient)
            .credentials("ak", "sk")
            .build();

    Assertions.assertEquals(42L, client.getObjectAttributes("bucket1", "folder/a.txt").block().objectSize());
    Assertions.assertEquals(
        ObjectRetentionConfiguration.GOVERNANCE,
        client.getObjectRetention("bucket1", "folder/a.txt", "v1").block().mode());
    client
        .setObjectRetention(
            "bucket1",
            "folder/a.txt",
            "v1",
            ObjectRetentionConfiguration.governance("2030-01-01T00:00:00Z"))
        .block();
    Assertions.assertTrue(client.getObjectLegalHold("bucket1", "folder/a.txt", "v2").block().enabledValue());
    client.setObjectLegalHold("bucket1", "folder/a.txt", "v2", ObjectLegalHoldConfiguration.enabled()).block();
    client.restoreObject("bucket1", "folder/a.txt", "v3", RestoreObjectRequest.of(5, "Bulk")).block();

    Assertions.assertTrue(paths.contains("/bucket1/folder/a.txt"));
    Assertions.assertTrue(containsAllQueryParts(queries, "attributes"));
    Assertions.assertTrue(containsAllQueryParts(queries, "retention", "versionId=v1"));
    Assertions.assertTrue(containsAllQueryParts(queries, "legal-hold", "versionId=v2"));
    Assertions.assertTrue(containsAllQueryParts(queries, "restore", "versionId=v3"));
    Assertions.assertTrue(objectAttributeHeaders.contains("ETag,ObjectSize,StorageClass,Checksum,ObjectParts"));
  }


  @Test
  void shouldExposeS3BucketSubresourceBusinessMethods() {
    assertMonoMethodExists(ReactiveMinioClient.class, "getBucketCorsConfiguration");
    assertMonoMethodExists(ReactiveMinioClient.class, "setBucketCorsConfiguration");
    assertMonoMethodExists(ReactiveMinioClient.class, "deleteBucketCorsConfiguration");
    assertMonoMethodExists(ReactiveMinioClient.class, "getBucketWebsiteConfiguration");
    assertMonoMethodExists(ReactiveMinioClient.class, "deleteBucketWebsiteConfiguration");
    assertMonoMethodExists(ReactiveMinioClient.class, "getBucketLoggingConfiguration");
    assertMonoMethodExists(ReactiveMinioClient.class, "getBucketPolicyStatus");
    assertMonoMethodExists(ReactiveMinioClient.class, "getBucketAccelerateConfiguration");
    assertMonoMethodExists(ReactiveMinioClient.class, "getBucketRequestPaymentConfiguration");
    assertAllPublicOverloadsDeprecated(ReactiveMinioClient.class, "s3PutBucketCors");
    assertAllPublicOverloadsDeprecated(ReactiveMinioClient.class, "s3DeleteBucketCors");
    assertAllPublicOverloadsDeprecated(ReactiveMinioClient.class, "s3DeleteBucketWebsite");
    assertDeprecatedMethodExists(ReactiveMinioClient.class, "s3GetBucketCors");
    assertDeprecatedMethodExists(ReactiveMinioClient.class, "s3GetBucketWebsite");
    assertDeprecatedMethodExists(ReactiveMinioClient.class, "s3GetBucketLogging");
    assertDeprecatedMethodExists(ReactiveMinioClient.class, "s3GetBucketPolicyStatus");
    assertDeprecatedMethodExists(ReactiveMinioClient.class, "s3GetBucketAccelerate");
    assertDeprecatedMethodExists(ReactiveMinioClient.class, "s3GetBucketRequestPayment");
  }

  @Test
  void shouldExposeS3AclAndSelectBusinessMethods() {
    assertMonoMethodExists(ReactiveMinioClient.class, "getObjectAcl");
    assertMonoMethodExists(ReactiveMinioClient.class, "setObjectCannedAcl");
    assertMonoMethodExists(ReactiveMinioClient.class, "getBucketAcl");
    assertMonoMethodExists(ReactiveMinioClient.class, "setBucketCannedAcl");
    assertMonoMethodExists(ReactiveMinioClient.class, "selectObjectContent");
    assertDeprecatedMethodExists(ReactiveMinioClient.class, "s3GetObjectAcl");
    assertAllPublicOverloadsDeprecated(ReactiveMinioClient.class, "s3PutObjectAcl");
    assertDeprecatedMethodExists(ReactiveMinioClient.class, "s3GetBucketAcl");
    assertAllPublicOverloadsDeprecated(ReactiveMinioClient.class, "s3PutBucketAcl");
    assertAllPublicOverloadsDeprecated(ReactiveMinioClient.class, "s3SelectObjectContent");
  }

  @Test
  void shouldExposeS3NotificationAndReplicationMetricsMethods() {
    assertMonoMethodExists(ReactiveMinioClient.class, "getBucketNotificationConfiguration");
    assertMonoMethodExists(ReactiveMinioClient.class, "setBucketNotificationConfiguration");
    assertFluxMethodExists(ReactiveMinioClient.class, "listenBucketNotification");
    assertFluxMethodExists(ReactiveMinioClient.class, "listenRootNotification");
    assertMonoMethodExists(ReactiveMinioClient.class, "getBucketReplicationMetrics");
    assertMonoMethodExists(ReactiveMinioClient.class, "getBucketReplicationMetricsV2");
    assertDeprecatedMethodExists(ReactiveMinioClient.class, "s3GetBucketNotification");
    assertAllPublicOverloadsDeprecated(ReactiveMinioClient.class, "s3PutBucketNotification");
    assertDeprecatedMethodExists(ReactiveMinioClient.class, "s3GetBucketReplicationMetrics");
    assertDeprecatedMethodExists(ReactiveMinioClient.class, "s3GetBucketReplicationMetricsV2");
  }


  @Test
  void shouldBuildS3BucketSubresourceRequests() {
    java.util.List<String> paths = new java.util.ArrayList<String>();
    java.util.List<String> queries = new java.util.ArrayList<String>();
    WebClient webClient =
        WebClient.builder()
            .exchangeFunction(
                request -> {
                  paths.add(request.url().getPath());
                  queries.add(request.url().getQuery());
                  return Mono.just(
                      ClientResponse.create(HttpStatus.OK)
                          .body(stage21BucketResponseBody(request.url().getQuery()))
                          .build());
                })
            .build();
    ReactiveMinioClient client =
        ReactiveMinioClient.builder()
            .endpoint("http://localhost:9000")
            .region("us-east-1")
            .webClient(webClient)
            .credentials("ak", "sk")
            .build();

    BucketCorsConfiguration cors =
        BucketCorsConfiguration.of(
            java.util.Arrays.asList(
                new BucketCorsRule(
                    java.util.Arrays.asList("GET"),
                    java.util.Arrays.asList("*"),
                    java.util.Arrays.asList("Authorization"),
                    java.util.Arrays.asList("ETag"),
                    60)));
    client.setBucketCorsConfiguration("bucket1", cors).block();
    Assertions.assertEquals("GET", client.getBucketCorsConfiguration("bucket1").block().rules().get(0).allowedMethods().get(0));
    client.deleteBucketCorsConfiguration("bucket1").block();
    Assertions.assertEquals("index.html", client.getBucketWebsiteConfiguration("bucket1").block().indexDocumentSuffix());
    client.deleteBucketWebsiteConfiguration("bucket1").block();
    Assertions.assertEquals("logs", client.getBucketLoggingConfiguration("bucket1").block().targetBucket());
    Assertions.assertTrue(client.getBucketPolicyStatus("bucket1").block().publicBucket());
    Assertions.assertTrue(client.getBucketAccelerateConfiguration("bucket1").block().enabled());
    Assertions.assertTrue(client.getBucketRequestPaymentConfiguration("bucket1").block().requesterPays());

    Assertions.assertTrue(paths.contains("/bucket1"));
    Assertions.assertTrue(containsAllQueryParts(queries, "cors"));
    Assertions.assertTrue(containsAllQueryParts(queries, "website"));
    Assertions.assertTrue(containsAllQueryParts(queries, "logging"));
    Assertions.assertTrue(containsAllQueryParts(queries, "policyStatus"));
    Assertions.assertTrue(containsAllQueryParts(queries, "accelerate"));
    Assertions.assertTrue(containsAllQueryParts(queries, "requestPayment"));
  }

  @Test
  void shouldBuildS3AclAndSelectRequests() {
    java.util.List<String> paths = new java.util.ArrayList<String>();
    java.util.List<String> queries = new java.util.ArrayList<String>();
    java.util.List<String> aclHeaders = new java.util.ArrayList<String>();
    WebClient webClient =
        WebClient.builder()
            .exchangeFunction(
                request -> {
                  paths.add(request.url().getPath());
                  queries.add(request.url().getQuery());
                  aclHeaders.add(request.headers().getFirst("x-amz-acl"));
                  return Mono.just(
                      ClientResponse.create(HttpStatus.OK)
                          .body(stage27S3ResponseBody(request.url().getQuery()))
                          .build());
                })
            .build();
    ReactiveMinioClient client =
        ReactiveMinioClient.builder()
            .endpoint("http://localhost:9000")
            .region("us-east-1")
            .webClient(webClient)
            .credentials("ak", "sk")
            .build();

    Assertions.assertEquals("owner-id", client.getObjectAcl("bucket1", "a.csv").block().owner().id());
    client.setObjectCannedAcl("bucket1", "a.csv", CannedAcl.PRIVATE).block();
    Assertions.assertTrue(client.getBucketAcl("bucket1").block().hasGrant("FULL_CONTROL"));
    client.setBucketCannedAcl("bucket1", CannedAcl.PUBLIC_READ).block();
    Assertions.assertEquals(
        "select-response",
        client
            .selectObjectContent("bucket1", "a.csv", SelectObjectContentRequest.csv("select * from s3object"))
            .block()
            .rawResponse());

    Assertions.assertTrue(paths.contains("/bucket1/a.csv"));
    Assertions.assertTrue(paths.contains("/bucket1"));
    Assertions.assertTrue(containsAllQueryParts(queries, "acl"));
    Assertions.assertTrue(containsAllQueryParts(queries, "select", "select-type=2"));
    Assertions.assertTrue(aclHeaders.contains("private"));
    Assertions.assertTrue(aclHeaders.contains("public-read"));
    Assertions.assertThrows(IllegalArgumentException.class, () -> client.setBucketCannedAcl("bucket1", null));
  }

  @Test
  void shouldBuildS3NotificationAndReplicationMetricsRequests() {
    java.util.List<String> paths = new java.util.ArrayList<String>();
    java.util.List<String> queries = new java.util.ArrayList<String>();
    WebClient webClient =
        WebClient.builder()
            .exchangeFunction(
                request -> {
                  paths.add(request.url().getPath());
                  queries.add(request.url().getQuery());
                  return Mono.just(
                      ClientResponse.create(HttpStatus.OK)
                          .body(stage28S3ResponseBody(request.url().getQuery()))
                          .build());
                })
            .build();
    ReactiveMinioClient client =
        ReactiveMinioClient.builder()
            .endpoint("http://localhost:9000")
            .region("us-east-1")
            .webClient(webClient)
            .credentials("ak", "sk")
            .build();

    BucketNotificationConfiguration configuration =
        BucketNotificationConfiguration.of(
            java.util.Arrays.asList(
                BucketNotificationTarget.queue(
                    "arn:minio:sqs::1:webhook", java.util.Arrays.asList("s3:ObjectCreated:*"))));
    client.setBucketNotificationConfiguration("bucket1", configuration).block();
    Assertions.assertEquals(
        "arn:minio:sqs::1:webhook",
        client.getBucketNotificationConfiguration("bucket1").block().targets().get(0).arn());
    Assertions.assertEquals(
        "event-stream",
        new String(
            client.listenBucketNotification("bucket1", "s3:ObjectCreated:*").blockFirst(),
            java.nio.charset.StandardCharsets.UTF_8));
    Assertions.assertEquals(
        "event-stream",
        new String(
            client.listenRootNotification("s3:ObjectRemoved:*").blockFirst(),
            java.nio.charset.StandardCharsets.UTF_8));
    BucketReplicationMetrics metrics = client.getBucketReplicationMetrics("bucket1").block();
    BucketReplicationMetrics metricsV2 = client.getBucketReplicationMetricsV2("bucket1").block();

    Assertions.assertEquals("v1", metrics.version());
    Assertions.assertEquals("v2", metricsV2.version());
    Assertions.assertEquals(123L, metricsV2.uptime());
    Assertions.assertTrue(paths.contains("/bucket1"));
    Assertions.assertTrue(paths.contains("/"));
    Assertions.assertTrue(containsAllQueryParts(queries, "notification"));
    Assertions.assertTrue(containsAllQueryParts(queries, "events=s3:ObjectCreated:*"));
    Assertions.assertTrue(containsAllQueryParts(queries, "events=s3:ObjectRemoved:*"));
    Assertions.assertTrue(containsAllQueryParts(queries, "replication-metrics"));
    Assertions.assertTrue(containsAllQueryParts(queries, "replication-metrics=2"));
  }


  private static void assertAllPublicOverloadsDeprecated(Class<?> type, String name) {
    int matched = 0;
    for (Method method : type.getMethods()) {
      if (method.getDeclaringClass().equals(type) && method.getName().equals(name)) {
        matched++;
        Assertions.assertNotNull(
            method.getAnnotation(Deprecated.class),
            "缺少 @Deprecated 重载: " + type.getSimpleName() + "." + name + java.util.Arrays.toString(method.getParameterTypes()));
      }
    }
    Assertions.assertTrue(matched > 0, "缺少方法: " + type.getSimpleName() + "." + name);
  }

  private static void assertAdvancedBaseline(
      Class<?> type, int monoStringCount, int deprecatedCount, int rawishCount) {
    Assertions.assertEquals(monoStringCount, countPublicMonoStringMethods(type), type.getSimpleName());
    Assertions.assertEquals(deprecatedCount, countDeprecatedMethods(type), type.getSimpleName());
    Assertions.assertEquals(rawishCount, countRawishExecuteMethods(type), type.getSimpleName());
  }

  private static int countPublicMonoStringMethods(Class<?> type) {
    int count = 0;
    for (Method method : type.getDeclaredMethods()) {
      if (Modifier.isPublic(method.getModifiers())
          && method.getReturnType().equals(Mono.class)
          && method.getGenericReturnType().toString().contains("java.lang.String")) {
        count++;
      }
    }
    return count;
  }

  private static int countDeprecatedMethods(Class<?> type) {
    int count = 0;
    for (Method method : type.getDeclaredMethods()) {
      if (Modifier.isPublic(method.getModifiers()) && method.getAnnotation(Deprecated.class) != null) {
        count++;
      }
    }
    return count;
  }

  private static int countRawishExecuteMethods(Class<?> type) {
    int count = 0;
    for (Method method : type.getDeclaredMethods()) {
      if (Modifier.isPublic(method.getModifiers()) && method.getName().startsWith("executeTo")) {
        count++;
      }
    }
    return count;
  }

  private static void assertDeprecatedMethodExists(Class<?> type, String name) {
    for (Method method : type.getMethods()) {
      if (method.getName().equals(name) && method.getAnnotation(Deprecated.class) != null) {
        return;
      }
    }
    Assertions.fail("缺少 @Deprecated 方法: " + type.getSimpleName() + "." + name);
  }

  private static boolean containsAllQueryParts(java.util.List<String> queries, String... parts) {
    for (String query : queries) {
      boolean matched = true;
      for (String part : parts) {
        matched = matched && query != null && query.contains(part);
      }
      if (matched) {
        return true;
      }
    }
    return false;
  }

  private static String stage15AdminResponseBody(String path) {
    if (path.endsWith("/help-config-kv")) {
      return "{\"subSys\":\"api\",\"description\":\"API 配置\",\"keysHelp\":[]}";
    }
    if (path.endsWith("/idp/builtin/policy-entities")) {
      return "{\"UserMappings\":{\"user1\":[\"readonly\"]},\"GroupMappings\":{\"dev\":[\"readwrite\"]},\"PolicyMappings\":{\"readonly\":[\"user1\"]}}";
    }
    if (path.endsWith("/idp-config/openid")) {
      return "{\"primary\":{}}";
    }
    if (path.endsWith("/idp-config/openid/primary")) {
      return "{\"status\":\"ok\"}";
    }
    if (path.endsWith("/storageinfo")) {
      return "{\"Disks\":[],\"Backend\":{\"Type\":1,\"OnlineDisks\":{},\"OfflineDisks\":{}}}";
    }
    if (path.endsWith("/datausageinfo")) {
      return "{\"objectsCount\":0,\"objectsTotalSize\":0,\"bucketsCount\":0}";
    }
    if (path.endsWith("/accountinfo")) {
      return "{\"AccountName\":\"root\",\"Server\":{\"Type\":1},\"Buckets\":[]}";
    }
    if (path.endsWith("/info-access-key")) {
      return "{\"accessKey\":\"svc1\",\"accountStatus\":\"enabled\"}";
    }
    if (path.endsWith("/list-access-keys-bulk")) {
      return "{\"serviceAccounts\":[],\"stsKeys\":[]}";
    }
    if (path.endsWith("/get-bucket-quota")) {
      return "{\"quota\":0,\"size\":0,\"rate\":0,\"requests\":0,\"quotatype\":\"hard\"}";
    }
    if (path.endsWith("/tier")) {
      return "[]";
    }
    if (path.endsWith("/list-remote-targets")) {
      return "{\"Targets\":[{\"Arn\":\"arn:minio:replication::bucket:target\",\"Type\":\"replication\",\"Endpoint\":\"http://remote\",\"Secure\":true}]}";
    }
    if (path.endsWith("/list-jobs")) {
      return "{\"Jobs\":[{\"ID\":\"job-1\",\"Type\":\"replicate\",\"Status\":\"running\",\"User\":\"root\"}]}";
    }
    if (path.endsWith("/status-job")) {
      return "{\"status\":\"running\"}";
    }
    if (path.endsWith("/describe-job")) {
      return "{\"id\":\"job-1\",\"status\":\"running\"}";
    }
    if (path.endsWith("/background-heal/status")
        || path.endsWith("/pools/list")
        || path.endsWith("/pools/status")
        || path.endsWith("/rebalance/status")
        || path.endsWith("/tier-stats")
        || path.endsWith("/site-replication/info")
        || path.endsWith("/site-replication/metainfo")
        || path.endsWith("/site-replication/status")
        || path.endsWith("/top/locks")
        || path.endsWith("/obdinfo")
        || path.endsWith("/healthinfo")) {
      return "{\"status\":\"ok\"}";
    }
    if (path.endsWith("/trace")) {
      return "trace-line\n";
    }
    if (path.endsWith("/log")) {
      return "log-line\n";
    }
    return "01234567890123456789012345678901234567890";
  }

  private static String stsSuccessXml() {
    return "<AssumeRoleResponse><AssumeRoleResult><Credentials>"
        + "<AccessKeyId>sts-access</AccessKeyId>"
        + "<SecretAccessKey>sts-secret</SecretAccessKey>"
        + "<SessionToken>sts-token</SessionToken>"
        + "<Expiration>2030-01-01T00:00:00Z</Expiration>"
        + "</Credentials></AssumeRoleResult></AssumeRoleResponse>";
  }

  private static String stage20S3ResponseBody(String query) {
    if (query != null && query.contains("attributes")) {
      return "<GetObjectAttributesOutput><ETag>\"etag\"</ETag><ObjectSize>42</ObjectSize><StorageClass>STANDARD</StorageClass></GetObjectAttributesOutput>";
    }
    if (query != null && query.contains("retention")) {
      return "<Retention><Mode>GOVERNANCE</Mode><RetainUntilDate>2030-01-01T00:00:00Z</RetainUntilDate></Retention>";
    }
    if (query != null && query.contains("legal-hold")) {
      return "<LegalHold><Status>ON</Status></LegalHold>";
    }
    return "";
  }

  private static String stage21BucketResponseBody(String query) {
    if (query != null && query.contains("cors")) {
      return "<CORSConfiguration><CORSRule><AllowedOrigin>*</AllowedOrigin><AllowedMethod>GET</AllowedMethod><AllowedHeader>Authorization</AllowedHeader><ExposeHeader>ETag</ExposeHeader><MaxAgeSeconds>60</MaxAgeSeconds></CORSRule></CORSConfiguration>";
    }
    if (query != null && query.contains("website")) {
      return "<WebsiteConfiguration><IndexDocument><Suffix>index.html</Suffix></IndexDocument></WebsiteConfiguration>";
    }
    if (query != null && query.contains("logging")) {
      return "<BucketLoggingStatus><LoggingEnabled><TargetBucket>logs</TargetBucket><TargetPrefix>app/</TargetPrefix></LoggingEnabled></BucketLoggingStatus>";
    }
    if (query != null && query.contains("policyStatus")) {
      return "<PolicyStatus><IsPublic>true</IsPublic></PolicyStatus>";
    }
    if (query != null && query.contains("accelerate")) {
      return "<AccelerateConfiguration><Status>Enabled</Status></AccelerateConfiguration>";
    }
    if (query != null && query.contains("requestPayment")) {
      return "<RequestPaymentConfiguration><Payer>Requester</Payer></RequestPaymentConfiguration>";
    }
    return "";
  }

  private static String stage27S3ResponseBody(String query) {
    if (query != null && query.contains("acl")) {
      return "<AccessControlPolicy>"
          + "<Owner><ID>owner-id</ID><DisplayName>root</DisplayName></Owner>"
          + "<AccessControlList><Grant><Grantee xsi:type=\"CanonicalUser\"><ID>owner-id</ID><DisplayName>root</DisplayName></Grantee><Permission>FULL_CONTROL</Permission></Grant></AccessControlList>"
          + "</AccessControlPolicy>";
    }
    if (query != null && query.contains("select")) {
      return "select-response";
    }
    return "";
  }

  private static String stage28S3ResponseBody(String query) {
    if (query != null && query.contains("events=")) {
      return "event-stream";
    }
    if (query != null && query.contains("notification")) {
      return "<NotificationConfiguration><QueueConfiguration><Id>queue-1</Id><Queue>arn:minio:sqs::1:webhook</Queue><Event>s3:ObjectCreated:*</Event></QueueConfiguration></NotificationConfiguration>";
    }
    if (query != null && query.contains("replication-metrics=2")) {
      return "{\"uptime\":123,\"Stats\":{}}";
    }
    if (query != null && query.contains("replication-metrics")) {
      return "{\"Stats\":{}}";
    }
    return "";
  }

  private static void assertMonoMethodExists(Class<?> type, String name) {
    for (Method method : type.getMethods()) {
      if (method.getName().equals(name) && method.getReturnType().equals(Mono.class)) {
        return;
      }
    }
    Assertions.fail("缺少返回 Mono 的方法: " + type.getSimpleName() + "." + name);
  }

  private static void assertFluxMethodExists(Class<?> type, String name) {
    for (Method method : type.getMethods()) {
      if (method.getName().equals(name) && method.getReturnType().equals(reactor.core.publisher.Flux.class)) {
        return;
      }
    }
    Assertions.fail("缺少返回 Flux 的方法: " + type.getSimpleName() + "." + name);
  }

  private static int countDistinctMonoMethods(Class<?> type, String prefix) {
    Set<String> names = new HashSet<String>();
    for (Method method : type.getMethods()) {
      if (method.getDeclaringClass().equals(type)
          && method.getReturnType().equals(Mono.class)
          && (prefix == null || method.getName().startsWith(prefix))) {
        names.add(method.getName());
      }
    }
    return names.size();
  }
}
