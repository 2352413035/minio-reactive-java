package io.minio.reactive.messages.admin;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** 更新用户组成员的请求对象。 */
public final class UpdateGroupMembersRequest {
  private final String group;
  private final List<String> members;
  private final String status;
  private final boolean remove;

  private UpdateGroupMembersRequest(String group, List<String> members, String status, boolean remove) {
    if (group == null || group.trim().isEmpty()) {
      throw new IllegalArgumentException("group 不能为空");
    }
    this.group = group;
    this.members = Collections.unmodifiableList(new ArrayList<String>(members == null ? Collections.<String>emptyList() : members));
    this.status = status == null || status.trim().isEmpty() ? "enabled" : status;
    this.remove = remove;
  }

  public static UpdateGroupMembersRequest add(String group, List<String> members) {
    return new UpdateGroupMembersRequest(group, members, "enabled", false);
  }

  public static UpdateGroupMembersRequest remove(String group, List<String> members) {
    return new UpdateGroupMembersRequest(group, members, "enabled", true);
  }

  public Map<String, Object> toPayload() {
    Map<String, Object> payload = new LinkedHashMap<String, Object>();
    payload.put("group", group);
    payload.put("members", members);
    payload.put("groupStatus", status);
    payload.put("isRemove", remove);
    return payload;
  }

  public String group() { return group; }
  public List<String> members() { return members; }
  public String status() { return status; }
  public boolean remove() { return remove; }
}
