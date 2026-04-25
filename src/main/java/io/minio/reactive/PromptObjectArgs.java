package io.minio.reactive;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/** PromptObject 请求参数对象。 */
public final class PromptObjectArgs extends ObjectVersionArgs {
  private final String lambdaArn;
  private final String prompt;
  private final Map<String, Object> promptArgs;
  private final Map<String, String> headers;

  private PromptObjectArgs(Builder builder) {
    super(builder);
    this.lambdaArn = requireText("lambdaArn", builder.lambdaArn);
    this.prompt = requireText("prompt", builder.prompt);
    this.promptArgs = copyObjectMap(builder.promptArgs);
    this.headers = builder.headers == null
        ? Collections.<String, String>emptyMap()
        : copyStringMap(builder.headers, "headers");
  }

  public String lambdaArn() { return lambdaArn; }
  public String prompt() { return prompt; }
  public Map<String, Object> promptArgs() { return promptArgs; }
  public Map<String, String> headers() { return headers; }

  public static Builder builder() { return new Builder(); }

  public static final class Builder extends ObjectVersionArgs.AbstractBuilder<Builder> {
    private String lambdaArn;
    private String prompt;
    private Map<String, Object> promptArgs;
    private Map<String, String> headers;

    public Builder lambdaArn(String lambdaArn) { this.lambdaArn = lambdaArn; return this; }
    public Builder prompt(String prompt) { this.prompt = prompt; return this; }
    public Builder promptArgs(Map<String, Object> promptArgs) { this.promptArgs = promptArgs; return this; }
    public Builder headers(Map<String, String> headers) { this.headers = headers; return this; }
    @Override protected Builder self() { return this; }
    public PromptObjectArgs build() { return new PromptObjectArgs(this); }
  }

  private static Map<String, Object> copyObjectMap(Map<String, Object> values) {
    if (values == null || values.isEmpty()) {
      return Collections.<String, Object>emptyMap();
    }
    Map<String, Object> result = new LinkedHashMap<String, Object>();
    for (Map.Entry<String, Object> entry : values.entrySet()) {
      if (entry.getKey() != null) {
        result.put(entry.getKey(), entry.getValue());
      }
    }
    return Collections.unmodifiableMap(result);
  }
}
