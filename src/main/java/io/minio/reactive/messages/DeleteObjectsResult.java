package io.minio.reactive.messages;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** 批量删除对象响应的解析结果。 */
public final class DeleteObjectsResult {
  private final List<DeletedObject> deletedObjects;

  public DeleteObjectsResult(List<DeletedObject> deletedObjects) {
    this.deletedObjects = Collections.unmodifiableList(new ArrayList<DeletedObject>(deletedObjects));
  }

  public List<DeletedObject> deletedObjects() {
    return deletedObjects;
  }
}
