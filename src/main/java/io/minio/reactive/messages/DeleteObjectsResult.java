package io.minio.reactive.messages;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Parsed DeleteObjects response. */
public final class DeleteObjectsResult {
  private final List<DeletedObject> deletedObjects;

  public DeleteObjectsResult(List<DeletedObject> deletedObjects) {
    this.deletedObjects = Collections.unmodifiableList(new ArrayList<DeletedObject>(deletedObjects));
  }

  public List<DeletedObject> deletedObjects() {
    return deletedObjects;
  }
}
