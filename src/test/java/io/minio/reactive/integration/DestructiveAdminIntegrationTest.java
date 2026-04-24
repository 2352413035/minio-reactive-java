package io.minio.reactive.integration;

import io.minio.reactive.ReactiveMinioAdminClient;
import io.minio.reactive.ReactiveMinioRawClient;
import io.minio.reactive.catalog.MinioApiCatalog;
import io.minio.reactive.messages.admin.AdminBatchJobList;
import io.minio.reactive.messages.admin.AdminJsonResult;
import io.minio.reactive.messages.admin.AdminRemoteTargetList;
import io.minio.reactive.messages.admin.AdminTierList;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
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

    try {
      admin.setConfigKvText(testConfigKv).block();
      Assertions.assertNotNull(admin.getConfigHelp(configSubSystem(testConfigKv)).block());
    } finally {
      admin.setConfigKvText(restoreConfigKv).block();
    }
    Assertions.assertNotNull(admin.getConfigHelp(configSubSystem(restoreConfigKv)).block());
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
    try {
      admin.setBucketQuota(bucket, bytes(testQuotaJson), "application/json").block();
      Assertions.assertNotNull(admin.getBucketQuotaInfo(bucket).block());
    } finally {
      admin.setBucketQuota(bucket, bytes(restoreQuotaJson), "application/json").block();
    }
    Assertions.assertNotNull(admin.getBucketQuotaInfo(bucket).block());
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
    Assumptions.assumeTrue(
        notBlank(tierName) || notBlank(bucket) || batchProbes,
        "缺少 tier/remote target/batch job lab fixture，跳过可选 lab 探测。");

    ReactiveMinioAdminClient admin = labAdminClient();
    ReactiveMinioRawClient raw = labRawClient();
    if (notBlank(tierName)) {
      AdminTierList typedTiers = admin.listTiers().block();
      String rawTiers = rawString(raw, "ADMIN_LIST_TIER");
      Assertions.assertNotNull(typedTiers);
      Assertions.assertEquals(AdminTierList.parse(rawTiers).tierCount(), typedTiers.tierCount());
      if ("true".equalsIgnoreCase(labValue("MINIO_LAB_EXPECT_TIER_IN_LIST"))) {
        Assertions.assertTrue(
            containsTierName(typedTiers, tierName),
            "MINIO_LAB_EXPECT_TIER_IN_LIST=true 时，listTiers() 必须能看到指定 tier: " + tierName);
      }
      Assertions.assertNotNull(admin.verifyTier(tierName).block());
    }
    if (notBlank(bucket)) {
      AdminRemoteTargetList typedTargets =
          admin.listRemoteTargetsInfo(bucket, remoteTargetType).block();
      String rawTargets =
          rawString(raw, "ADMIN_LIST_REMOTE_TARGETS", map("bucket", bucket, "type", remoteTargetType));
      Assertions.assertNotNull(typedTargets);
      Assertions.assertEquals(
          AdminRemoteTargetList.parse(rawTargets).targetCount(), typedTargets.targetCount());
      if (notBlank(expectedRemoteTargetArn)) {
        Assertions.assertTrue(
            containsRemoteTargetArn(typedTargets, expectedRemoteTargetArn),
            "MINIO_LAB_REMOTE_TARGET_EXPECTED_ARN 已设置，但 typed remote target 摘要未找到该 ARN。");
      }
    }
    if (batchProbes) {
      AdminBatchJobList typedJobs = admin.listBatchJobsInfo().block();
      String rawJobs = rawString(raw, "ADMIN_LIST_BATCH_JOBS");
      Assertions.assertNotNull(typedJobs);
      Assertions.assertEquals(AdminBatchJobList.parse(rawJobs).jobCount(), typedJobs.jobCount());

      AdminJsonResult typedStatus = admin.getBatchJobStatusInfo().block();
      String rawStatus = rawString(raw, "ADMIN_BATCH_JOB_STATUS");
      Assertions.assertNotNull(typedStatus);
      Assertions.assertEquals(AdminJsonResult.parse(rawStatus).rawJson(), typedStatus.rawJson());

      AdminJsonResult typedDescription = admin.describeBatchJobInfo().block();
      String rawDescription = rawString(raw, "ADMIN_DESCRIBE_BATCH_JOB");
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
        && notBlank(labValue("MINIO_LAB_REMOVE_REMOTE_TARGET_ARN"))
        && "true".equalsIgnoreCase(labValue("MINIO_LAB_REMOVE_REMOTE_TARGET_AFTER_TEST"));
  }

  private static boolean hasBatchJobWriteFixture() {
    return hasBody("MINIO_LAB_BATCH_START_BODY", "MINIO_LAB_BATCH_START_BODY_FILE")
        && hasBody("MINIO_LAB_BATCH_CANCEL_BODY", "MINIO_LAB_BATCH_CANCEL_BODY_FILE")
        && "true".equalsIgnoreCase(labValue("MINIO_LAB_CANCEL_BATCH_AFTER_TEST"));
  }

  private static boolean hasSiteReplicationWriteFixture() {
    return hasBody("MINIO_LAB_SITE_REPLICATION_ADD_BODY", "MINIO_LAB_SITE_REPLICATION_ADD_BODY_FILE")
        && hasBody(
            "MINIO_LAB_SITE_REPLICATION_REMOVE_BODY",
            "MINIO_LAB_SITE_REPLICATION_REMOVE_BODY_FILE")
        && "true".equalsIgnoreCase(labValue("MINIO_LAB_REMOVE_SITE_REPLICATION_AFTER_TEST"));
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
      admin.addTier(bytes(addBody), contentType).block();
      Assertions.assertNotNull(admin.listTiers().block());
      rawString(raw, "ADMIN_REMOVE_TIER", map("tier", tier), emptyMap(), null, null);
      rawString(raw, "ADMIN_ADD_TIER", emptyMap(), emptyMap(), bytes(addBody), contentType);
      Assertions.assertNotNull(admin.listTiers().block());
      if (notBlank(editBody)) {
        admin.editTier(tier, bytes(editBody), contentType).block();
        rawString(
            raw, "ADMIN_EDIT_TIER", map("tier", tier), emptyMap(), bytes(editBody), contentType);
        Assertions.assertNotNull(admin.listTiers().block());
      }
    } finally {
      admin.removeTier(tier)
          .onErrorResume(error -> reactor.core.publisher.Mono.empty())
          .block();
    }
    Assertions.assertNotNull(admin.listTiers().block());
  }

  private static void exerciseRemoteTargetWriteFixture(
      ReactiveMinioAdminClient admin, ReactiveMinioRawClient raw) {
    String bucket = labValue("MINIO_LAB_REMOTE_TARGET_WRITE_BUCKET");
    String body =
        labBody("MINIO_LAB_SET_REMOTE_TARGET_BODY", "MINIO_LAB_SET_REMOTE_TARGET_BODY_FILE");
    String arn = labValue("MINIO_LAB_REMOVE_REMOTE_TARGET_ARN");
    String type = getenvOrDefault("MINIO_LAB_REMOTE_TARGET_TYPE", "replication");
    String contentType =
        getenvOrDefault("MINIO_LAB_REMOTE_TARGET_WRITE_CONTENT_TYPE", "application/json");
    try {
      // remote target 夹具同样执行“专用客户端写入 + raw 兜底写入 + 专用客户端恢复”的闭环。
      admin.setRemoteTarget(bucket, bytes(body), contentType).block();
      Assertions.assertNotNull(admin.listRemoteTargetsInfo(bucket, type).block());
      rawString(
          raw,
          "ADMIN_REMOVE_REMOTE_TARGET",
          emptyMap(),
          map("bucket", bucket, "arn", arn),
          null,
          null);
      rawString(
          raw,
          "ADMIN_SET_REMOTE_TARGET",
          emptyMap(),
          map("bucket", bucket),
          bytes(body),
          contentType);
      Assertions.assertNotNull(admin.listRemoteTargetsInfo(bucket, type).block());
    } finally {
      admin.removeRemoteTarget(bucket, arn)
          .onErrorResume(error -> reactor.core.publisher.Mono.empty())
          .block();
    }
    Assertions.assertNotNull(admin.listRemoteTargetsInfo(bucket, type).block());
  }

  private static void exerciseBatchJobWriteMatrix(
      ReactiveMinioAdminClient admin, ReactiveMinioRawClient raw) {
    String startBody =
        labBody("MINIO_LAB_BATCH_START_BODY", "MINIO_LAB_BATCH_START_BODY_FILE");
    String cancelBody =
        labBody("MINIO_LAB_BATCH_CANCEL_BODY", "MINIO_LAB_BATCH_CANCEL_BODY_FILE");
    String contentType = getenvOrDefault("MINIO_LAB_BATCH_JOB_CONTENT_TYPE", "application/yaml");
    try {
      // batch job 只在独立 lab 中启动；取消动作同时覆盖 raw 兜底路径。
      admin.startBatchJob(bytes(startBody), contentType).block();
      Assertions.assertNotNull(admin.listBatchJobsInfo().block());
      Assertions.assertNotNull(admin.getBatchJobStatusInfo().block());
      rawString(raw, "ADMIN_CANCEL_BATCH_JOB", emptyMap(), emptyMap(), bytes(cancelBody), contentType);
    } finally {
      admin.cancelBatchJob(bytes(cancelBody), contentType)
          .onErrorResume(error -> reactor.core.publisher.Mono.empty())
          .block();
    }
    Assertions.assertNotNull(admin.listBatchJobsInfo().block());
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
      admin.siteReplicationAdd(bytes(addBody), contentType).block();
      Assertions.assertNotNull(admin.getSiteReplicationInfo().block());
      if (notBlank(editBody)) {
        rawString(
            raw,
            "ADMIN_SITE_REPLICATION_EDIT",
            emptyMap(),
            emptyMap(),
            bytes(editBody),
            contentType);
      }
      rawString(
          raw,
          "ADMIN_SITE_REPLICATION_REMOVE",
          emptyMap(),
          emptyMap(),
          bytes(removeBody),
          contentType);
    } finally {
      admin.siteReplicationRemove(bytes(removeBody), contentType)
          .onErrorResume(error -> reactor.core.publisher.Mono.empty())
          .block();
    }
    Assertions.assertNotNull(admin.getSiteReplicationStatus().block());
  }

  private static Map<String, String> map(String... values) {
    Map<String, String> result = new LinkedHashMap<String, String>();
    for (int i = 0; i + 1 < values.length; i += 2) {
      result.put(values[i], values[i + 1]);
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
