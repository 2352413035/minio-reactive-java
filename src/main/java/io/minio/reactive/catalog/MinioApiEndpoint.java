package io.minio.reactive.catalog;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Describes one HTTP interface exposed by the MinIO server routers. */
public final class MinioApiEndpoint {
  private final String name;
  private final String family;
  private final String method;
  private final String pathTemplate;
  private final boolean authRequired;
  private final String authScheme;
  private final Map<String, String> defaultQueryParameters;
  private final List<String> requiredQueryParameters;

  MinioApiEndpoint(
      String name,
      String family,
      String method,
      String pathTemplate,
      boolean authRequired,
      Map<String, String> defaultQueryParameters,
      List<String> requiredQueryParameters) {
    this.name = name;
    this.family = family;
    this.method = method;
    this.pathTemplate = pathTemplate;
    this.authRequired = authRequired;
    this.authScheme = authRequired ? "sigv4" : "none";
    this.defaultQueryParameters =
        Collections.unmodifiableMap(new LinkedHashMap<String, String>(defaultQueryParameters));
    this.requiredQueryParameters =
        Collections.unmodifiableList(new ArrayList<String>(requiredQueryParameters));
  }


  MinioApiEndpoint(
      String name,
      String family,
      String method,
      String pathTemplate,
      String authScheme,
      Map<String, String> defaultQueryParameters,
      List<String> requiredQueryParameters) {
    this.name = name;
    this.family = family;
    this.method = method;
    this.pathTemplate = pathTemplate;
    this.authScheme = authScheme == null ? "none" : authScheme;
    this.authRequired = !"none".equals(this.authScheme);
    this.defaultQueryParameters =
        Collections.unmodifiableMap(new LinkedHashMap<String, String>(defaultQueryParameters));
    this.requiredQueryParameters =
        Collections.unmodifiableList(new ArrayList<String>(requiredQueryParameters));
  }

  public String name() {
    return name;
  }

  public String family() {
    return family;
  }

  public String method() {
    return method;
  }

  public String pathTemplate() {
    return pathTemplate;
  }

  public boolean authRequired() {
    return authRequired;
  }

  public String authScheme() {
    return authScheme;
  }

  public boolean requiresSigV4() {
    return "sigv4".equals(authScheme);
  }

  public Map<String, String> defaultQueryParameters() {
    return defaultQueryParameters;
  }

  public List<String> requiredQueryParameters() {
    return requiredQueryParameters;
  }

  @Override
  public String toString() {
    return method + " " + pathTemplate + " [" + name + "]";
  }
}
