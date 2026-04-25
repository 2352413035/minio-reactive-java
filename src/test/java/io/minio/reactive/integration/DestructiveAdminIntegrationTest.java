package io.minio.reactive.integration;

import io.minio.reactive.ReactiveMinioAdminClient;
import io.minio.reactive.ReactiveMinioRawClient;
import io.minio.reactive.catalog.MinioApiCatalog;
import io.minio.reactive.messages.admin.AdminBatchJobList;
import io.minio.reactive.messages.admin.AdminBatchJobStartResult;
import io.minio.reactive.messages.admin.AdminBucketQuota;
import io.minio.reactive.messages.admin.AdminJsonResult;
import io.minio.reactive.messages.admin.AdminDriveSpeedtestOptions;
import io.minio.reactive.messages.admin.AdminSpeedtestOptions;
import io.minio.reactive.messages.admin.AdminRemoteTargetList;
import io.minio.reactive.messages.admin.AdminTierList;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.Duration;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

/**
 * 破坏性 Admin 集成测试入口。
 *
 * <p>这类测试会修改 MinIO 服务端配置或远端目标，默认必须跳过；只有独立可回滚环境才能显式开启。
 */
class DestructiveAdminIntegrationTest {
  private static Map<String, String> cachedLabConfig;

  private interface LabRunnable {
    void run() throws Exception;
  }

  private interface LabSupplier<T> {
    T get() throws Exception;
  }

  @Test
  void shouldRequireVerifiedLabEnvironmentBeforeDestructiveSuite() throws Exception {
    assumeDestructiveLabEnabled();
    assertVerifiedLabEnvironment();
  }

  @Test
  void shouldWriteAndRestoreConfigKvOnlyInsideVerifiedLab() throws Exception {
    assumeDestructiveLabEnabled();
    assertVerifiedLabEnvironment();
    String testConfigKv = labValue("MINIO_LAB_TEST_CONFIG_KV");
    String restoreConfigKv = labValue("MINIO_LAB_RESTORE_CONFIG_KV");
    Assumptions.assumeTrue(
        testConfigKv != null
            && !testConfigKv.trim().isEmpty()
            && restoreConfigKv != null
            && !restoreConfigKv.trim().isEmpty(),
        "缺少 MINIO_LAB_TEST_CONFIG_KV/MINIO_LAB_RESTORE_CONFIG_KV，跳过真实 config 写入与恢复。");

    ReactiveMinioAdminClient admin = labAdminClient();
    ReactiveMinioRawClient raw = labRawClient();

    try {
      runLabStep("config", "typed setConfigKvText 测试值", () -> admin.setConfigKvText(testConfigKv).block());
      labStepValue(
          "config",
          "typed getConfigHelp 测试值",
          () -> admin.getConfigHelp(configSubSystem(testConfigKv)).block());
      runLabStep(
          "config",
          "raw ADMIN_SET_CONFIG_KV 测试值",
          () ->
              rawVoid(
                  raw,
                  "ADMIN_SET_CONFIG_KV",
                  emptyMap(),
                  emptyMap(),
                  encryptedLabBody(testConfigKv),
                  "application/octet-stream"));
      labStepValue(
          "config",
          "raw ADMIN_HELP_CONFIG_KV 测试值",
          () ->
              rawString(
                  raw,
                  "ADMIN_HELP_CONFIG_KV",
                  emptyMap(),
                  map("subSys", configSubSystem(testConfigKv), "key", ""),
                  null,
                  null));
    } finally {
      try {
        runLabStep(
            "config",
            "raw ADMIN_SET_CONFIG_KV 恢复值",
            () ->
                rawVoid(
                    raw,
                    "ADMIN_SET_CONFIG_KV",
                    emptyMap(),
                    emptyMap(),
                    encryptedLabBody(restoreConfigKv),
                    "application/octet-stream"));
      } finally {
        runLabStep("config", "typed setConfigKvText 恢复值", () -> admin.setConfigKvText(restoreConfigKv).block());
      }
    }
    Assertions.assertNotNull(
        labStepValue(
            "config",
            "typed getConfigHelp 恢复后",
            () -> admin.getConfigHelp(configSubSystem(restoreConfigKv)).block()));
  }

  @Test
  void shouldRewriteFullConfigOnlyInsideVerifiedLab() throws Exception {
    assumeDestructiveLabEnabled();
    assertVerifiedLabEnvironment();
    Assumptions.assumeTrue(
        "true".equalsIgnoreCase(labValue("MINIO_LAB_ALLOW_FULL_CONFIG_WRITE")),
        "缺少 MINIO_LAB_ALLOW_FULL_CONFIG_WRITE=true，跳过全量配置原样写回验证。");

    ReactiveMinioAdminClient admin = labAdminClient();
    ReactiveMinioRawClient raw = labRawClient();
    String secretKey = labValue("MINIO_LAB_SECRET_KEY");
    String originalConfig =
        labStepValue(
            "full-config",
            "typed getConfigDecrypted 原始全量配置",
            () -> admin.getConfigDecrypted(secretKey).block());
    Assumptions.assumeTrue(notBlank(originalConfig), "全量配置为空，跳过全量配置写回验证。");

    try {
      // 全量配置写入风险比单条 KV 更高，所以 lab 只做“原样写回”闭环，
      // 证明 SDK 的 typed/raw 路径可用，但不改变配置语义。
      runLabStep("full-config", "typed setConfigText 原样写回", () -> admin.setConfigText(originalConfig).block());
      Assertions.assertTrue(
          notBlank(
              labStepValue(
                  "full-config",
                  "typed getConfigDecrypted typed 写回后",
                  () -> admin.getConfigDecrypted(secretKey).block())),
          "typed 全量配置写回后应仍可读取配置。");
      runLabStep(
          "full-config",
          "raw ADMIN_SET_CONFIG 原样写回",
          () ->
              rawVoid(
                  raw,
                  "ADMIN_SET_CONFIG",
                  emptyMap(),
                  emptyMap(),
                  encryptedLabBody(originalConfig),
                  "application/octet-stream"));
      Assertions.assertTrue(
          notBlank(
              labStepValue(
                  "full-config",
                  "typed getConfigDecrypted raw 写回后",
                  () -> admin.getConfigDecrypted(secretKey).block())),
          "raw 全量配置写回后应仍可读取配置。");
    } finally {
      runLabStep(
          "full-config-restore",
          "typed setConfigText 恢复原始全量配置",
          () ->
              admin.setConfigText(originalConfig)
                  .onErrorResume(error -> reactor.core.publisher.Mono.empty())
                  .block());
    }
  }

  @Test
  void shouldWriteAndRestoreBucketQuotaOnlyInsideVerifiedLab() throws Exception {
    assumeDestructiveLabEnabled();
    assertVerifiedLabEnvironment();
    String bucket = labValue("MINIO_LAB_BUCKET");
    String testQuotaJson = labValue("MINIO_LAB_TEST_BUCKET_QUOTA_JSON");
    String restoreQuotaJson = labValue("MINIO_LAB_RESTORE_BUCKET_QUOTA_JSON");
    Assumptions.assumeTrue(
        notBlank(bucket) && notBlank(testQuotaJson) && notBlank(restoreQuotaJson),
        "缺少 bucket quota lab 配置，跳过真实 bucket quota 写入与恢复。");

    ReactiveMinioAdminClient admin = labAdminClient();
    ReactiveMinioRawClient raw = labRawClient();
    try {
      runLabStep(
          "bucket-quota",
          "typed setBucketQuota 测试值",
          () -> admin.setBucketQuota(bucket, bytes(testQuotaJson), "application/json").block());
      labStepValue(
          "bucket-quota", "typed getBucketQuotaInfo 测试值", () -> admin.getBucketQuotaInfo(bucket).block());
      runLabStep(
          "bucket-quota",
          "raw ADMIN_SET_BUCKET_QUOTA 测试值",
          () ->
              rawVoid(
                  raw,
                  "ADMIN_SET_BUCKET_QUOTA",
                  emptyMap(),
                  map("bucket", bucket),
                  bytes(testQuotaJson),
                  "application/json"));
      Assertions.assertNotNull(
          labStepValue(
              "bucket-quota",
              "raw ADMIN_GET_BUCKET_QUOTA 测试值",
              () ->
                  AdminBucketQuota.parse(
                      rawString(raw, "ADMIN_GET_BUCKET_QUOTA", map("bucket", bucket)))));
    } finally {
      try {
        runLabStep(
            "bucket-quota",
            "raw ADMIN_SET_BUCKET_QUOTA 恢复值",
            () ->
                rawVoid(
                    raw,
                    "ADMIN_SET_BUCKET_QUOTA",
                    emptyMap(),
                    map("bucket", bucket),
                    bytes(restoreQuotaJson),
                    "application/json"));
      } finally {
        runLabStep(
            "bucket-quota",
            "typed setBucketQuota 恢复值",
            () -> admin.setBucketQuota(bucket, bytes(restoreQuotaJson), "application/json").block());
      }
    }
    Assertions.assertNotNull(
        labStepValue(
            "bucket-quota", "typed getBucketQuotaInfo 恢复后", () -> admin.getBucketQuotaInfo(bucket).block()));
  }

  @Test
  void shouldProbeTierRemoteTargetAndBatchFixturesOnlyInsideVerifiedLab() throws Exception {
    assumeDestructiveLabEnabled();
    assertVerifiedLabEnvironment();
    String tierName = labValue("MINIO_LAB_TIER_NAME");
    String bucket = labValue("MINIO_LAB_BUCKET");
    String remoteTargetType = getenvOrDefault("MINIO_LAB_REMOTE_TARGET_TYPE", "replication");
    String expectedRemoteTargetArn = labValue("MINIO_LAB_REMOTE_TARGET_EXPECTED_ARN");
    String expectedBatchJobId = labValue("MINIO_LAB_BATCH_EXPECTED_JOB_ID");
    boolean batchProbes = "true".equalsIgnoreCase(labValue("MINIO_LAB_ENABLE_BATCH_JOB_PROBES"));
    boolean replicationDiffProbe =
        "true".equalsIgnoreCase(labValue("MINIO_LAB_ENABLE_REPLICATION_DIFF_PROBE"));
    Assumptions.assumeTrue(
        notBlank(tierName) || notBlank(bucket) || batchProbes || replicationDiffProbe,
        "缺少 tier/remote target/batch job/replication diff lab fixture，跳过可选 lab 探测。");

    ReactiveMinioAdminClient admin = labAdminClient();
    ReactiveMinioRawClient raw = labRawClient();
    if (notBlank(tierName)) {
      AdminTierList typedTiers =
          labStepValue("tier-probe", "typed listTiers", () -> admin.listTiers().block());
      String rawTiers =
          labStepValue("tier-probe", "raw ADMIN_LIST_TIER", () -> rawString(raw, "ADMIN_LIST_TIER"));
      Assertions.assertNotNull(typedTiers);
      Assertions.assertEquals(AdminTierList.parse(rawTiers).tierCount(), typedTiers.tierCount());
      if ("true".equalsIgnoreCase(labValue("MINIO_LAB_EXPECT_TIER_IN_LIST"))) {
        Assertions.assertTrue(
            containsTierName(typedTiers, tierName),
            "MINIO_LAB_EXPECT_TIER_IN_LIST=true 时，listTiers() 必须能看到指定 tier: " + tierName);
      }
      Assertions.assertNotNull(
          labStepValue("tier-probe", "typed verifyTier", () -> admin.verifyTier(tierName).block()));
    }
    if (notBlank(bucket)) {
      AdminRemoteTargetList typedTargets =
          labStepValue(
              "remote-target-probe",
              "typed listRemoteTargetsInfo",
              () -> admin.listRemoteTargetsInfo(bucket, remoteTargetType).block());
      String rawTargets =
          labStepValue(
              "remote-target-probe",
              "raw ADMIN_LIST_REMOTE_TARGETS",
              () -> rawString(raw, "ADMIN_LIST_REMOTE_TARGETS", map("bucket", bucket, "type", remoteTargetType)));
      Assertions.assertNotNull(typedTargets);
      Assertions.assertEquals(
          AdminRemoteTargetList.parse(rawTargets).targetCount(), typedTargets.targetCount());
      if (notBlank(expectedRemoteTargetArn)) {
        Assertions.assertTrue(
            containsRemoteTargetArn(typedTargets, expectedRemoteTargetArn),
            "MINIO_LAB_REMOTE_TARGET_EXPECTED_ARN 已设置，但 typed remote target 摘要未找到该 ARN。");
      }
    }
    if (replicationDiffProbe) {
      Assumptions.assumeTrue(notBlank(bucket), "replication diff 探测需要 MINIO_LAB_BUCKET。");
      String prefix = labValue("MINIO_LAB_REPLICATION_DIFF_PREFIX");
      String arn = labValue("MINIO_LAB_REPLICATION_DIFF_ARN");
      String typedDiff =
          labStepValue(
              "replication-diff-probe",
              "typed runReplicationDiff",
              () -> admin.runReplicationDiff(bucket, true, prefix, arn).block().rawText());
      String rawDiff =
          labStepValue(
              "replication-diff-probe",
              "raw ADMIN_REPLICATION_DIFF",
              () ->
                  rawString(
                      raw,
                      "ADMIN_REPLICATION_DIFF",
                      emptyMap(),
                      replicationDiffQuery(bucket, true, prefix, arn),
                      null,
                      null));
      Assertions.assertNotNull(typedDiff);
      Assertions.assertEquals(typedDiff, rawDiff);
    }
    if (batchProbes) {
      AdminBatchJobList typedJobs =
          labStepValue("batch-probe", "typed listBatchJobsInfo", () -> admin.listBatchJobsInfo().block());
      String rawJobs =
          labStepValue("batch-probe", "raw ADMIN_LIST_BATCH_JOBS", () -> rawString(raw, "ADMIN_LIST_BATCH_JOBS"));
      Assertions.assertNotNull(typedJobs);
      Assertions.assertEquals(AdminBatchJobList.parse(rawJobs).jobCount(), typedJobs.jobCount());

      AdminJsonResult typedStatus =
          labStepValue(
              "batch-probe", "typed getBatchJobStatusInfo", () -> admin.getBatchJobStatusInfo().block());
      String rawStatus =
          labStepValue(
              "batch-probe", "raw ADMIN_BATCH_JOB_STATUS", () -> rawString(raw, "ADMIN_BATCH_JOB_STATUS"));
      Assertions.assertNotNull(typedStatus);
      Assertions.assertEquals(AdminJsonResult.parse(rawStatus).rawJson(), typedStatus.rawJson());

      AdminJsonResult typedDescription =
          labStepValue(
              "batch-probe", "typed describeBatchJobInfo", () -> admin.describeBatchJobInfo().block());
      String rawDescription =
          labStepValue(
              "batch-probe", "raw ADMIN_DESCRIBE_BATCH_JOB", () -> rawString(raw, "ADMIN_DESCRIBE_BATCH_JOB"));
      Assertions.assertNotNull(typedDescription);
      Assertions.assertEquals(
          AdminJsonResult.parse(rawDescription).rawJson(), typedDescription.rawJson());

      if (notBlank(expectedBatchJobId)) {
        Assertions.assertTrue(
            containsBatchJobId(typedJobs, expectedBatchJobId)
                || typedStatus.rawJson().contains(expectedBatchJobId)
                || typedDescription.rawJson().contains(expectedBatchJobId),
            "MINIO_LAB_BATCH_EXPECTED_JOB_ID 已设置，但 batch job typed/raw 摘要均未找到该任务。");
      }
    }
  }

  @Test
  void shouldExerciseIdpConfigMatrixOnlyInsideVerifiedLab() throws Exception {
    assumeDestructiveLabEnabled();
    assertVerifiedLabEnvironment();
    boolean writeAllowed = "true".equalsIgnoreCase(labValue("MINIO_LAB_ALLOW_WRITE_FIXTURES"));
    Assumptions.assumeTrue(
        writeAllowed && hasIdpWriteFixture(),
        "缺少 MINIO_LAB_ALLOW_WRITE_FIXTURES=true 或 IDP add/delete 夹具，跳过 IDP 实验矩阵。");

    ReactiveMinioAdminClient admin = labAdminClient();
    ReactiveMinioRawClient raw = labRawClient();
    exerciseIdpWriteFixture(admin, raw);
  }

  @Test
  void shouldRunBoundedSpeedtestProbesOnlyInsideVerifiedLab() throws Exception {
    assumeDestructiveLabEnabled();
    assertVerifiedLabEnvironment();
    Assumptions.assumeTrue(
        "true".equalsIgnoreCase(labValue("MINIO_LAB_ENABLE_SPEEDTEST_PROBES")),
        "缺少 MINIO_LAB_ENABLE_SPEEDTEST_PROBES=true，跳过 speedtest 资源压测探测。");

    ReactiveMinioAdminClient admin = labAdminClient();
    ReactiveMinioRawClient raw = labRawClient();
    AdminSpeedtestOptions objectOptions =
        AdminSpeedtestOptions.builder()
            .sizeBytes(intLabValue("MINIO_LAB_SPEEDTEST_OBJECT_SIZE", 1048576))
            .concurrency(intLabValue("MINIO_LAB_SPEEDTEST_OBJECT_CONCURRENCY", 1))
            .duration(Duration.ofSeconds(intLabValue("MINIO_LAB_SPEEDTEST_OBJECT_DURATION_SECONDS", 2)))
            .bucket(labValue("MINIO_LAB_BUCKET"))
            .build();
    String typedCluster =
        labStepValue(
            "speedtest-probe",
            "typed runSpeedtest bounded",
            () -> admin.runSpeedtest(objectOptions).block().rawText());
    String rawCluster =
        labStepValue(
            "speedtest-probe",
            "raw ADMIN_SPEEDTEST bounded",
            () ->
                rawString(
                    raw,
                    "ADMIN_SPEEDTEST",
                    emptyMap(),
                    objectOptions.toQueryParameters(),
                    null,
                    null));
    String typedObject =
        labStepValue(
            "speedtest-probe",
            "typed runObjectSpeedtest bounded",
            () -> admin.runObjectSpeedtest(objectOptions).block().rawText());
    String rawObject =
        labStepValue(
            "speedtest-probe",
            "raw ADMIN_SPEEDTEST_OBJECT bounded",
            () ->
                rawString(
                    raw,
                    "ADMIN_SPEEDTEST_OBJECT",
                    emptyMap(),
                    objectOptions.toQueryParameters(),
                    null,
                    null));
    Assertions.assertTrue(notBlank(typedCluster), "typed cluster speedtest 应返回结果。");
    Assertions.assertTrue(notBlank(rawCluster), "raw cluster speedtest 应返回结果。");
    Assertions.assertTrue(notBlank(typedObject), "typed object speedtest 应返回结果。");
    Assertions.assertTrue(notBlank(rawObject), "raw object speedtest 应返回结果。");

    if ("true".equalsIgnoreCase(labValue("MINIO_LAB_ENABLE_DRIVE_SPEEDTEST_PROBE"))) {
      AdminDriveSpeedtestOptions driveOptions =
          AdminDriveSpeedtestOptions.builder()
              .serial(true)
              .blockSizeBytes(longLabValue("MINIO_LAB_SPEEDTEST_DRIVE_BLOCK_SIZE", 4096L))
              .fileSizeBytes(longLabValue("MINIO_LAB_SPEEDTEST_DRIVE_FILE_SIZE", 8192L))
              .build();
      String typedDrive =
          labStepValue(
              "speedtest-probe",
              "typed runDriveSpeedtest bounded",
              () -> admin.runDriveSpeedtest(driveOptions).block().rawText());
      Assertions.assertTrue(notBlank(typedDrive), "typed drive speedtest 应返回结果。");
    }
  }

  @Test
  void shouldWriteAndRestoreTierAndRemoteTargetOnlyInsideVerifiedLab() throws Exception {
    assumeDestructiveLabEnabled();
    assertVerifiedLabEnvironment();
    boolean writeAllowed = "true".equalsIgnoreCase(labValue("MINIO_LAB_ALLOW_WRITE_FIXTURES"));
    boolean tierFixture = hasTierWriteFixture();
    boolean remoteFixture = hasRemoteTargetWriteFixture();
    Assumptions.assumeTrue(
        writeAllowed && (tierFixture || remoteFixture),
        "缺少 MINIO_LAB_ALLOW_WRITE_FIXTURES=true 或 tier/remote target 写入夹具，跳过真实写入与恢复。");

    ReactiveMinioAdminClient admin = labAdminClient();
    ReactiveMinioRawClient raw = labRawClient();
    boolean exercised = false;
    if (tierFixture) {
      exerciseTierWriteFixture(admin, raw);
      exercised = true;
    }
    if (remoteFixture) {
      exerciseRemoteTargetWriteFixture(admin, raw);
      exercised = true;
    }
    Assertions.assertTrue(exercised, "至少需要执行一个可回滚写入夹具。");
  }

  @Test
  void shouldExerciseBatchAndSiteReplicationMatrixOnlyInsideVerifiedLab() throws Exception {
    assumeDestructiveLabEnabled();
    assertVerifiedLabEnvironment();
    boolean writeAllowed = "true".equalsIgnoreCase(labValue("MINIO_LAB_ALLOW_WRITE_FIXTURES"));
    boolean batchFixture = hasBatchJobWriteFixture();
    boolean siteFixture = hasSiteReplicationWriteFixture();
    Assumptions.assumeTrue(
        writeAllowed && (batchFixture || siteFixture),
        "缺少 MINIO_LAB_ALLOW_WRITE_FIXTURES=true 或 batch/site replication 写入夹具，跳过实验矩阵。");

    ReactiveMinioAdminClient admin = labAdminClient();
    ReactiveMinioRawClient raw = labRawClient();
    boolean exercised = false;
    if (batchFixture) {
      exerciseBatchJobWriteMatrix(admin, raw);
      exercised = true;
    }
    if (siteFixture) {
      exerciseSiteReplicationWriteMatrix(admin, raw);
      exercised = true;
    }
    Assertions.assertTrue(exercised, "至少需要执行一个 batch 或 site replication 实验矩阵。");
  }

  private static void assumeDestructiveLabEnabled() {
    Assumptions.assumeTrue(
        "true".equalsIgnoreCase(labValue("MINIO_ALLOW_DESTRUCTIVE_ADMIN_TESTS")),
        "破坏性 Admin 测试默认跳过；需要独立可回滚 MinIO 环境并设置 MINIO_ALLOW_DESTRUCTIVE_ADMIN_TESTS=true");
  }

  private static void assertVerifiedLabEnvironment() throws Exception {
    ProcessBuilder builder =
        new ProcessBuilder("bash", "scripts/minio-lab/verify-env.sh")
            .directory(new java.io.File(System.getProperty("user.dir")))
            .redirectErrorStream(true);
    String configPath = labConfigPath();
    if (notBlank(configPath)) {
      builder.environment().put("MINIO_LAB_CONFIG_FILE", configPath);
    }
    Process process = builder.start();
    StringBuilder output = new StringBuilder();
    try (BufferedReader reader =
        new BufferedReader(new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
      String line;
      while ((line = reader.readLine()) != null) {
        output.append(line).append('\n');
      }
    }
    int exitCode = process.waitFor();
    Assertions.assertEquals(0, exitCode, output.toString());
  }

  private static ReactiveMinioAdminClient labAdminClient() {
    return ReactiveMinioAdminClient.builder()
        .endpoint(labValue("MINIO_LAB_ENDPOINT"))
        .region(getenvOrDefault("MINIO_LAB_REGION", "us-east-1"))
        .credentials(labValue("MINIO_LAB_ACCESS_KEY"), labValue("MINIO_LAB_SECRET_KEY"))
        .build();
  }

  private static ReactiveMinioRawClient labRawClient() {
    return ReactiveMinioRawClient.builder()
        .endpoint(labValue("MINIO_LAB_ENDPOINT"))
        .region(getenvOrDefault("MINIO_LAB_REGION", "us-east-1"))
        .credentials(labValue("MINIO_LAB_ACCESS_KEY"), labValue("MINIO_LAB_SECRET_KEY"))
        .build();
  }

  private static boolean hasTierWriteFixture() {
    return notBlank(labValue("MINIO_LAB_TIER_WRITE_NAME"))
        && hasBody("MINIO_LAB_ADD_TIER_BODY", "MINIO_LAB_ADD_TIER_BODY_FILE")
        && "true".equalsIgnoreCase(labValue("MINIO_LAB_REMOVE_TIER_AFTER_TEST"));
  }

  private static boolean hasRemoteTargetWriteFixture() {
    return notBlank(labValue("MINIO_LAB_REMOTE_TARGET_WRITE_BUCKET"))
        && hasBody("MINIO_LAB_SET_REMOTE_TARGET_BODY", "MINIO_LAB_SET_REMOTE_TARGET_BODY_FILE")
        && "true".equalsIgnoreCase(labValue("MINIO_LAB_REMOVE_REMOTE_TARGET_AFTER_TEST"));
  }

  private static boolean hasBatchJobWriteFixture() {
    return hasBody("MINIO_LAB_BATCH_START_BODY", "MINIO_LAB_BATCH_START_BODY_FILE")
        && "true".equalsIgnoreCase(labValue("MINIO_LAB_CANCEL_BATCH_AFTER_TEST"));
  }

  private static boolean hasSiteReplicationWriteFixture() {
    return hasBody("MINIO_LAB_SITE_REPLICATION_ADD_BODY", "MINIO_LAB_SITE_REPLICATION_ADD_BODY_FILE")
        && hasBody(
            "MINIO_LAB_SITE_REPLICATION_REMOVE_BODY",
            "MINIO_LAB_SITE_REPLICATION_REMOVE_BODY_FILE")
        && "true".equalsIgnoreCase(labValue("MINIO_LAB_REMOVE_SITE_REPLICATION_AFTER_TEST"));
  }

  private static boolean hasIdpWriteFixture() {
    return hasBody("MINIO_LAB_ADD_IDP_CONFIG_BODY", "MINIO_LAB_ADD_IDP_CONFIG_BODY_FILE")
        && "true".equalsIgnoreCase(labValue("MINIO_LAB_DELETE_IDP_AFTER_TEST"));
  }

  private static void exerciseIdpWriteFixture(
      ReactiveMinioAdminClient admin, ReactiveMinioRawClient raw) {
    String type = getenvOrDefault("MINIO_LAB_IDP_TYPE", "openid");
    String name = getenvOrDefault("MINIO_LAB_IDP_NAME", "_");
    String addBody =
        labBody("MINIO_LAB_ADD_IDP_CONFIG_BODY", "MINIO_LAB_ADD_IDP_CONFIG_BODY_FILE");
    String updateBody =
        labBody("MINIO_LAB_UPDATE_IDP_CONFIG_BODY", "MINIO_LAB_UPDATE_IDP_CONFIG_BODY_FILE");
    try {
      // IDP add/update 请求体在专用客户端内自动加密；raw 兜底路径必须由调用方显式加密。
      runLabStep(
          "idp-config-write",
          "typed addIdpConfig",
          () -> admin.addIdpConfigEntry(type, name, bytes(addBody), null).block());
      Assertions.assertNotNull(
          labStepValue("idp-config-write", "typed listIdpConfigs after add", () -> admin.listIdpConfigs(type).block()));
      Assertions.assertNotNull(
          labStepValue(
              "idp-config-write",
              "typed getIdpConfigInfo after add",
              () -> admin.getIdpConfigInfo(type, name).block()));
      labStepValue(
          "idp-config-write",
          "raw ADMIN_DELETE_IDP_CONFIG",
          () -> rawString(raw, "ADMIN_DELETE_IDP_CONFIG", map("type", type, "name", name), emptyMap(), null, null));
      labStepValue(
          "idp-config-write",
          "raw ADMIN_ADD_IDP_CONFIG",
          () ->
              rawString(
                  raw,
                  "ADMIN_ADD_IDP_CONFIG",
                  map("type", type, "name", name),
                  emptyMap(),
                  encryptedLabBody(addBody),
                  "application/octet-stream"));
      Assertions.assertNotNull(
          labStepValue("idp-config-write", "typed listIdpConfigs after raw add", () -> admin.listIdpConfigs(type).block()));
      if (notBlank(updateBody)) {
        runLabStep(
            "idp-config-write",
            "typed updateIdpConfig",
            () -> admin.updateIdpConfigEntry(type, name, bytes(updateBody), null).block());
        labStepValue(
            "idp-config-write",
            "raw ADMIN_UPDATE_IDP_CONFIG",
            () ->
                rawString(
                    raw,
                    "ADMIN_UPDATE_IDP_CONFIG",
                    map("type", type, "name", name),
                    emptyMap(),
                    encryptedLabBody(updateBody),
                    "application/octet-stream"));
      }
    } finally {
      runLabStep(
          "idp-config-restore",
          "typed deleteIdpConfig finally",
          () ->
              admin.deleteIdpConfig(type, name)
                  .onErrorResume(error -> reactor.core.publisher.Mono.empty())
                  .block());
    }
  }

  private static void exerciseTierWriteFixture(
      ReactiveMinioAdminClient admin, ReactiveMinioRawClient raw) {
    String tier = labValue("MINIO_LAB_TIER_WRITE_NAME");
    String addBody = labBody("MINIO_LAB_ADD_TIER_BODY", "MINIO_LAB_ADD_TIER_BODY_FILE");
    String editBody = labBody("MINIO_LAB_EDIT_TIER_BODY", "MINIO_LAB_EDIT_TIER_BODY_FILE");
    String contentType = getenvOrDefault("MINIO_LAB_TIER_WRITE_CONTENT_TYPE", "application/json");
    try {
      // 先走专用 Admin 客户端，再走 raw catalog 同一路由，
      // 证明两条入口都能在独立 lab 中写入并恢复。
      runLabStep("tier-write", "typed addTier", () -> admin.addTier(bytes(addBody), contentType).block());
      Assertions.assertNotNull(
          labStepValue("tier-write", "typed listTiers after add", () -> admin.listTiers().block()));
      labStepValue(
          "tier-write",
          "raw ADMIN_REMOVE_TIER",
          () -> rawString(raw, "ADMIN_REMOVE_TIER", map("tier", tier), emptyMap(), null, null));
      labStepValue(
          "tier-write",
          "raw ADMIN_ADD_TIER",
          () ->
              rawString(
                  raw,
                  "ADMIN_ADD_TIER",
                  emptyMap(),
                  emptyMap(),
                  encryptedLabBody(addBody),
                  "application/octet-stream"));
      Assertions.assertNotNull(
          labStepValue("tier-write", "typed listTiers after raw add", () -> admin.listTiers().block()));
      if (notBlank(editBody)) {
        runLabStep(
            "tier-write", "typed editTier", () -> admin.editTier(tier, bytes(editBody), contentType).block());
        labStepValue(
            "tier-write",
            "raw ADMIN_EDIT_TIER",
            () ->
                rawString(
                    raw,
                    "ADMIN_EDIT_TIER",
                    map("tier", tier),
                    emptyMap(),
                    encryptedLabBody(editBody),
                    "application/octet-stream"));
        Assertions.assertNotNull(
            labStepValue("tier-write", "typed listTiers after edit", () -> admin.listTiers().block()));
      }
    } finally {
      runLabStep(
          "tier-restore",
          "typed removeTier finally",
          () ->
              admin.removeTier(tier)
                  .onErrorResume(error -> reactor.core.publisher.Mono.empty())
                  .block());
    }
    Assertions.assertNotNull(
        labStepValue("tier-restore", "typed listTiers after restore", () -> admin.listTiers().block()));
  }

  private static void exerciseRemoteTargetWriteFixture(
      ReactiveMinioAdminClient admin, ReactiveMinioRawClient raw) {
    String bucket = labValue("MINIO_LAB_REMOTE_TARGET_WRITE_BUCKET");
    String body =
        labBody("MINIO_LAB_SET_REMOTE_TARGET_BODY", "MINIO_LAB_SET_REMOTE_TARGET_BODY_FILE");
    String[] restoreArn = {normalizeArn(labValue("MINIO_LAB_REMOVE_REMOTE_TARGET_ARN"))};
    String type = getenvOrDefault("MINIO_LAB_REMOTE_TARGET_TYPE", "replication");
    String contentType =
        getenvOrDefault("MINIO_LAB_REMOTE_TARGET_WRITE_CONTENT_TYPE", "application/json");
    try {
      // remote target set 会返回服务端生成的 ARN；优先用该 ARN 恢复，
      // 只有服务端未返回时才要求调用方通过 MINIO_LAB_REMOVE_REMOTE_TARGET_ARN 兜底。
      String typedArn =
          labStepValue(
              "remote-target-write",
              "typed setRemoteTarget",
              () -> normalizeArn(admin.setRemoteTarget(bucket, bytes(body), contentType).block()));
      restoreArn[0] = firstNonBlank(typedArn, restoreArn[0]);
      Assertions.assertTrue(
          notBlank(restoreArn[0]),
          "set remote target 未返回 ARN；请在独立 lab 配置中提供 MINIO_LAB_REMOVE_REMOTE_TARGET_ARN。");
      Assertions.assertNotNull(
          labStepValue(
              "remote-target-write",
              "typed listRemoteTargetsInfo after set",
              () -> admin.listRemoteTargetsInfo(bucket, type).block()));
      final String arnAfterTypedSet = restoreArn[0];
      labStepValue(
          "remote-target-write",
          "raw ADMIN_REMOVE_REMOTE_TARGET",
          () -> rawString(
              raw,
              "ADMIN_REMOVE_REMOTE_TARGET",
              emptyMap(),
              map("bucket", bucket, "arn", arnAfterTypedSet),
              null,
              null));
      String rawArn =
          labStepValue(
              "remote-target-write",
              "raw ADMIN_SET_REMOTE_TARGET",
              () ->
                  normalizeArn(
                      rawString(
                          raw,
                          "ADMIN_SET_REMOTE_TARGET",
                          emptyMap(),
                          map("bucket", bucket),
                          encryptedLabBody(body),
                          "application/octet-stream")));
      restoreArn[0] = firstNonBlank(rawArn, arnAfterTypedSet);
      Assertions.assertNotNull(
          labStepValue(
              "remote-target-write",
              "typed listRemoteTargetsInfo after raw set",
              () -> admin.listRemoteTargetsInfo(bucket, type).block()));
    } finally {
      final String arn = restoreArn[0];
      if (notBlank(arn)) {
        runLabStep(
            "remote-target-restore",
            "typed removeRemoteTarget finally",
            () ->
                admin.removeRemoteTarget(bucket, arn)
                    .onErrorResume(error -> reactor.core.publisher.Mono.empty())
                    .block());
      }
    }
    Assertions.assertNotNull(
        labStepValue(
            "remote-target-restore",
            "typed listRemoteTargetsInfo after restore",
            () -> admin.listRemoteTargetsInfo(bucket, type).block()));
  }

  private static void exerciseBatchJobWriteMatrix(
      ReactiveMinioAdminClient admin, ReactiveMinioRawClient raw) {
    String startBody =
        labBody("MINIO_LAB_BATCH_START_BODY", "MINIO_LAB_BATCH_START_BODY_FILE");
    String contentType = getenvOrDefault("MINIO_LAB_BATCH_JOB_CONTENT_TYPE", "application/yaml");
    final String[] jobId = {""};
    try {
      // batch job start 返回 jobId；MinIO madmin 的 status/cancel 都使用该 ID 查询参数。
      AdminBatchJobStartResult start =
          labStepValue(
              "batch-write",
              "typed startBatchJobInfo",
              () -> admin.startBatchJobInfo(bytes(startBody), contentType).block());
      Assertions.assertNotNull(start);
      jobId[0] = start.jobId();
      Assertions.assertTrue(notBlank(jobId[0]), "batch job 启动响应缺少 jobId，无法安全取消。");
      Assertions.assertNotNull(
          labStepValue("batch-write", "typed listBatchJobsInfo", () -> admin.listBatchJobsInfo().block()));
      Assertions.assertNotNull(
          labStepValue(
              "batch-write",
              "typed getBatchJobStatusInfo(jobId)",
              () -> admin.getBatchJobStatusInfo(jobId[0]).block()));
      labStepValue(
          "batch-write",
          "raw ADMIN_CANCEL_BATCH_JOB",
          () -> rawString(raw, "ADMIN_CANCEL_BATCH_JOB", emptyMap(), map("id", jobId[0]), null, null));
    } finally {
      final String cancelId = jobId[0];
      if (notBlank(cancelId)) {
        runLabStep(
            "batch-restore",
            "typed cancelBatchJob finally",
            () ->
                admin.cancelBatchJob(cancelId)
                    .onErrorResume(error -> reactor.core.publisher.Mono.empty())
                    .block());
      }
    }
    Assertions.assertNotNull(
        labStepValue("batch-restore", "typed listBatchJobsInfo after restore", () -> admin.listBatchJobsInfo().block()));
  }

  private static void exerciseSiteReplicationWriteMatrix(
      ReactiveMinioAdminClient admin, ReactiveMinioRawClient raw) {
    String addBody =
        labBody("MINIO_LAB_SITE_REPLICATION_ADD_BODY", "MINIO_LAB_SITE_REPLICATION_ADD_BODY_FILE");
    String editBody =
        labBody(
            "MINIO_LAB_SITE_REPLICATION_EDIT_BODY", "MINIO_LAB_SITE_REPLICATION_EDIT_BODY_FILE");
    String removeBody =
        labBody(
            "MINIO_LAB_SITE_REPLICATION_REMOVE_BODY",
            "MINIO_LAB_SITE_REPLICATION_REMOVE_BODY_FILE");
    String contentType =
        getenvOrDefault("MINIO_LAB_SITE_REPLICATION_CONTENT_TYPE", "application/json");
    try {
      // site replication 写入影响集群拓扑，必须由独立 lab 提供 add/remove 请求体。
      runLabStep(
          "site-replication-write",
          "typed siteReplicationAdd",
          () -> admin.siteReplicationAdd(bytes(addBody), contentType).block());
      Assertions.assertNotNull(
          labStepValue(
              "site-replication-write",
              "typed getSiteReplicationInfo after add",
              () -> admin.getSiteReplicationInfo().block()));
      Assertions.assertNotNull(
          labStepValue(
              "site-replication-write",
              "typed getSiteReplicationStatus after add",
              () -> admin.getSiteReplicationStatus().block()));
      Assertions.assertNotNull(
          labStepValue(
              "site-replication-write",
              "typed getSiteReplicationMetainfo after add",
              () -> admin.getSiteReplicationMetainfo().block()));
      if (notBlank(editBody)) {
        labStepValue(
            "site-replication-write",
            "raw ADMIN_SITE_REPLICATION_EDIT",
            () ->
                rawString(
                    raw,
                    "ADMIN_SITE_REPLICATION_EDIT",
                    emptyMap(),
                    map("api-version", "1"),
                    encryptedLabBody(editBody),
                    "application/octet-stream"));
      }
      labStepValue(
          "site-replication-write",
          "raw ADMIN_SITE_REPLICATION_REMOVE",
          () -> rawString(
              raw,
              "ADMIN_SITE_REPLICATION_REMOVE",
              emptyMap(),
              map("api-version", "1"),
              bytes(removeBody),
              contentType));
      labStepValue(
          "site-replication-write",
          "raw ADMIN_SITE_REPLICATION_ADD after remove",
          () -> rawString(
              raw,
              "ADMIN_SITE_REPLICATION_ADD",
              emptyMap(),
              map("api-version", "1"),
              encryptedLabBody(addBody),
              "application/octet-stream"));
      Assertions.assertNotNull(
          labStepValue(
              "site-replication-write",
              "typed getSiteReplicationInfo after raw add",
              () -> admin.getSiteReplicationInfo().block()));
    } finally {
      runLabStep(
          "site-replication-restore",
          "typed siteReplicationRemove finally",
          () ->
              admin.siteReplicationRemove(bytes(removeBody), contentType)
                  .onErrorResume(error -> reactor.core.publisher.Mono.empty())
                  .block());
    }
    Assertions.assertNotNull(
        labStepValue(
            "site-replication-restore",
            "typed getSiteReplicationStatus after restore",
            () -> admin.getSiteReplicationStatus().block()));
  }

  private static void runLabStep(String scope, String step, LabRunnable action) {
    labStepValue(
        scope,
        step,
        new LabSupplier<Void>() {
          @Override
          public Void get() throws Exception {
            action.run();
            return null;
          }
        });
  }

  private static <T> T labStepValue(String scope, String step, LabSupplier<T> supplier) {
    try {
      T value = supplier.get();
      recordLabStep(scope, step, "PASS", "");
      return value;
    } catch (Throwable failure) {
      recordLabStep(scope, step, "FAIL", failure.getClass().getSimpleName());
      if (failure instanceof RuntimeException) {
        throw (RuntimeException) failure;
      }
      if (failure instanceof Error) {
        throw (Error) failure;
      }
      throw new IllegalStateException("破坏性 lab 步骤执行失败: " + step, failure);
    }
  }

  private static void recordLabStep(String scope, String step, String status, String detail) {
    String file = labValue("MINIO_LAB_STEP_STATUS_FILE");
    if (!notBlank(file)) {
      return;
    }
    String line =
        safeStepCell(scope)
            + "|"
            + safeStepCell(step)
            + "|"
            + safeStepCell(status)
            + "|"
            + safeStepCell(detail)
            + "\n";
    try {
      Path path = Paths.get(file);
      Path parent = path.getParent();
      if (parent != null) {
        Files.createDirectories(parent);
      }
      Files.write(
          path,
          line.getBytes(StandardCharsets.UTF_8),
          StandardOpenOption.CREATE,
          StandardOpenOption.APPEND);
    } catch (Exception e) {
      throw new IllegalStateException("无法写入 destructive lab 步骤状态文件: " + file, e);
    }
  }

  private static String safeStepCell(String value) {
    if (value == null) {
      return "";
    }
    return value.replace('\n', ' ').replace('\r', ' ').replace('|', '／').trim();
  }

  private static Map<String, String> map(String... values) {
    Map<String, String> result = new LinkedHashMap<String, String>();
    for (int i = 0; i + 1 < values.length; i += 2) {
      result.put(values[i], values[i + 1]);
    }
    return result;
  }

  private static Map<String, String> replicationDiffQuery(
      String bucket, boolean verbose, String prefix, String arn) {
    Map<String, String> result = new LinkedHashMap<String, String>();
    result.put("bucket", bucket);
    if (verbose) {
      result.put("verbose", "true");
    }
    if (notBlank(prefix)) {
      result.put("prefix", prefix);
    }
    if (notBlank(arn)) {
      result.put("arn", arn);
    }
    return result;
  }

  private static Map<String, String> emptyMap() {
    return Collections.<String, String>emptyMap();
  }

  private static String rawString(ReactiveMinioRawClient raw, String endpointName) {
    return rawString(raw, endpointName, emptyMap());
  }

  private static String rawString(
      ReactiveMinioRawClient raw, String endpointName, Map<String, String> query) {
    return rawString(raw, endpointName, emptyMap(), query, null, null);
  }

  private static String rawString(
      ReactiveMinioRawClient raw,
      String endpointName,
      Map<String, String> pathVariables,
      Map<String, String> query,
      byte[] body,
      String contentType) {
    return raw
        .executeToString(
            MinioApiCatalog.byName(endpointName),
            pathVariables,
            query,
            emptyMap(),
            body,
            contentType)
        .block();
  }

  private static void rawVoid(
      ReactiveMinioRawClient raw,
      String endpointName,
      Map<String, String> pathVariables,
      Map<String, String> query,
      byte[] body,
      String contentType) {
    raw.executeToVoid(
            MinioApiCatalog.byName(endpointName),
            pathVariables,
            query,
            emptyMap(),
            body,
            contentType)
        .block();
  }

  private static byte[] encryptedLabBody(String plainText) {
    return encryptedLabBody(bytes(plainText));
  }

  private static byte[] encryptedLabBody(byte[] plainBody) {
    // MinIO madmin 写入类配置接口要求请求体用当前凭证 secretKey 加密。
    // raw 客户端不替用户做业务语义判断，所以 lab 里显式构造加密体来证明兜底能力。
    return io.minio.reactive.util.MadminEncryptionSupport.encryptData(
        labValue("MINIO_LAB_SECRET_KEY"), plainBody);
  }

  private static String normalizeArn(String response) {
    if (!notBlank(response)) {
      return "";
    }
    String trimmed = response.trim();
    try {
      com.fasterxml.jackson.databind.JsonNode root =
          io.minio.reactive.util.JsonSupport.parseTree(trimmed);
      if (root != null) {
        if (root.isTextual()) {
          return root.asText();
        }
        String arn = io.minio.reactive.util.JsonSupport.textAny(root, "arn", "Arn", "ARN");
        if (notBlank(arn)) {
          return arn;
        }
      }
    } catch (Exception ignored) {
      // 非 JSON 响应保留原文本，方便兼容旧版本或 mock。
    }
    return trimmed;
  }

  private static String firstNonBlank(String first, String second) {
    return notBlank(first) ? first : notBlank(second) ? second : "";
  }

  private static boolean containsTierName(AdminTierList tiers, String tierName) {
    if (tiers == null) {
      return false;
    }
    for (AdminTierList.Tier tier : tiers.tiers()) {
      if (tierName.equals(tier.name())) {
        return true;
      }
    }
    return tiers.rawJson().contains(tierName);
  }

  private static boolean containsRemoteTargetArn(
      AdminRemoteTargetList targets, String expectedRemoteTargetArn) {
    if (targets == null) {
      return false;
    }
    for (AdminRemoteTargetList.Target target : targets.targets()) {
      if (expectedRemoteTargetArn.equals(target.arn())) {
        return true;
      }
    }
    return targets.rawJson().contains(expectedRemoteTargetArn);
  }

  private static boolean containsBatchJobId(AdminBatchJobList jobs, String expectedBatchJobId) {
    if (jobs == null) {
      return false;
    }
    for (AdminBatchJobList.Job job : jobs.jobs()) {
      if (expectedBatchJobId.equals(job.id())) {
        return true;
      }
    }
    return jobs.rawJson().contains(expectedBatchJobId);
  }

  private static String configSubSystem(String kvText) {
    String trimmed = kvText == null ? "" : kvText.trim();
    int space = trimmed.indexOf(' ');
    int equals = trimmed.indexOf('=');
    int end = space < 0 ? equals : equals < 0 ? space : Math.min(space, equals);
    return end <= 0 ? trimmed : trimmed.substring(0, end);
  }

  private static int intLabValue(String name, int defaultValue) {
    String value = labValue(name);
    if (!notBlank(value)) {
      return defaultValue;
    }
    return Integer.parseInt(value.trim());
  }

  private static long longLabValue(String name, long defaultValue) {
    String value = labValue(name);
    if (!notBlank(value)) {
      return defaultValue;
    }
    return Long.parseLong(value.trim());
  }

  private static String getenvOrDefault(String name, String defaultValue) {
    String value = labValue(name);
    return value == null || value.trim().isEmpty() ? defaultValue : value;
  }

  private static byte[] bytes(String value) {
    return value.getBytes(StandardCharsets.UTF_8);
  }

  private static boolean hasBody(String bodyName, String fileName) {
    return notBlank(labValue(bodyName)) || notBlank(labValue(fileName));
  }

  private static String labBody(String bodyName, String fileName) {
    String inlineBody = labValue(bodyName);
    if (notBlank(inlineBody)) {
      return inlineBody;
    }
    String bodyFile = labValue(fileName);
    if (!notBlank(bodyFile)) {
      return "";
    }
    try {
      return new String(Files.readAllBytes(Paths.get(bodyFile)), StandardCharsets.UTF_8);
    } catch (Exception e) {
      throw new IllegalStateException("无法读取 destructive lab 请求体文件: " + bodyFile, e);
    }
  }

  private static boolean notBlank(String value) {
    return value != null && !value.trim().isEmpty();
  }

  private static String labValue(String name) {
    String value = System.getenv(name);
    if (notBlank(value)) {
      return value;
    }
    return labConfig().get(name);
  }

  private static Map<String, String> labConfig() {
    if (cachedLabConfig != null) {
      return cachedLabConfig;
    }
    String configPath = labConfigPath();
    if (!notBlank(configPath)) {
      cachedLabConfig = Collections.emptyMap();
      return cachedLabConfig;
    }
    Path path = Paths.get(configPath);
    if (!Files.isRegularFile(path)) {
      cachedLabConfig = Collections.emptyMap();
      return cachedLabConfig;
    }
    Map<String, String> values = new LinkedHashMap<String, String>();
    try {
      for (String line : Files.readAllLines(path, StandardCharsets.UTF_8)) {
        String trimmed = line.trim();
        if (trimmed.isEmpty() || trimmed.startsWith("#") || !trimmed.contains("=")) {
          continue;
        }
        String key = trimmed.substring(0, trimmed.indexOf('=')).trim();
        String value = trimmed.substring(trimmed.indexOf('=') + 1);
        if (!key.matches("[A-Z0-9_]+")) {
          continue;
        }
        values.put(key, stripOuterQuotes(value));
      }
    } catch (Exception e) {
      throw new IllegalStateException("无法读取 destructive lab 配置文件: " + configPath, e);
    }
    cachedLabConfig = values;
    return cachedLabConfig;
  }

  private static String labConfigPath() {
    String value = System.getenv("MINIO_LAB_CONFIG_FILE");
    if (notBlank(value)) {
      return value;
    }
    String property = System.getProperty("minio.lab.config");
    if (notBlank(property)) {
      return property;
    }
    Path defaultPath = Paths.get(System.getProperty("user.dir"), "scripts", "minio-lab", "lab.properties");
    return Files.isRegularFile(defaultPath) ? defaultPath.toString() : "";
  }

  private static String stripOuterQuotes(String value) {
    if (value == null) {
      return "";
    }
    String trimmed = value.trim();
    if (trimmed.length() >= 2
        && ((trimmed.startsWith("\"") && trimmed.endsWith("\""))
            || (trimmed.startsWith("'") && trimmed.endsWith("'")))) {
      return trimmed.substring(1, trimmed.length() - 1);
    }
    return trimmed;
  }
}
