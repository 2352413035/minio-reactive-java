package io.minio.reactive.messages.admin;

import com.fasterxml.jackson.databind.JsonNode;
import io.minio.reactive.util.JsonSupport;

/** 锁热点摘要，保留完整 JSON，同时统计读锁、写锁、最长持有时间和疑似未达 quorum 的锁。 */
public final class AdminTopLocksSummary extends AdminJsonResult {
  private final int lockCount;
  private final int readLockCount;
  private final int writeLockCount;
  private final int belowQuorumLockCount;
  private final long maxElapsedNanos;

  private AdminTopLocksSummary(
      String rawJson,
      int lockCount,
      int readLockCount,
      int writeLockCount,
      int belowQuorumLockCount,
      long maxElapsedNanos) {
    super(rawJson, JsonSupport.parseMap(rawJson));
    this.lockCount = lockCount;
    this.readLockCount = readLockCount;
    this.writeLockCount = writeLockCount;
    this.belowQuorumLockCount = belowQuorumLockCount;
    this.maxElapsedNanos = maxElapsedNanos;
  }

  public static AdminTopLocksSummary parse(String rawJson) {
    JsonNode locks = locksNode(JsonSupport.parseTree(rawJson));
    int readCount = 0;
    int writeCount = 0;
    int belowQuorumCount = 0;
    long maxElapsed = 0L;
    if (locks != null && locks.isArray()) {
      for (JsonNode lock : locks) {
        String type = JsonSupport.textAny(lock, "type", "Type").toLowerCase(java.util.Locale.ROOT);
        if ("read".equals(type)) {
          readCount++;
        } else if ("write".equals(type)) {
          writeCount++;
        }
        int quorum = JsonSupport.intAny(lock, "quorum", "Quorum");
        int serverCount = JsonSupport.nodeSize(JsonSupport.child(lock, "serverlist", "ServerList"));
        if (quorum > 0 && serverCount > 0 && serverCount < quorum) {
          belowQuorumCount++;
        }
        long elapsed = JsonSupport.longAny(lock, "elapsed", "Elapsed");
        if (elapsed > maxElapsed) {
          maxElapsed = elapsed;
        }
      }
    }
    return new AdminTopLocksSummary(
        rawJson, JsonSupport.nodeSize(locks), readCount, writeCount, belowQuorumCount, maxElapsed);
  }

  private static JsonNode locksNode(JsonNode root) {
    if (root != null && root.isArray()) {
      return root;
    }
    return JsonSupport.child(root, "locks", "Locks", "items");
  }

  public int lockCount() {
    return lockCount;
  }

  public int readLockCount() {
    return readLockCount;
  }

  public int writeLockCount() {
    return writeLockCount;
  }

  public int belowQuorumLockCount() {
    return belowQuorumLockCount;
  }

  public long maxElapsedNanos() {
    return maxElapsedNanos;
  }
}
