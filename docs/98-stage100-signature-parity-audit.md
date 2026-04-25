# 阶段 100：minio-java 签名级差异审计

## 目标

前面的名称级报告已经证明对象 API、Admin API、`*Args` 类名和 credentials provider 类名完成收口。阶段 100 增加签名级报告，用来回答更细的问题：同名方法是否还有重载数量差异，credentials provider 是否有构造器/工厂迁移边界，`*Args` 是否只有类名而缺少关键入口。

## 新增脚本

新增脚本：

```bash
python3 scripts/report-minio-java-signature-parity.py \
  --minio-java-root ../minio-java \
  --worktree . \
  --format markdown \
  --output ../.omx/reports/minio-java-signature-parity-jdk8.md
```

脚本只做源码静态扫描，不编译、不联网、不读取凭证。

## 当前审计结论

以当前 JDK8/JDK17+ 双分支报告为准：

- 对象存储 API：没有缺失方法，也没有“重载较少”的方法；当前 reactive 分支因为响应式便捷方法较多，报告中会出现“响应式扩展”。
- Admin API：没有缺失方法，也没有“重载较少”的方法。
- credentials provider：阻塞 HTTP 客户端相关构造器被明确归类为“响应式 SDK 有意不同”；其他 provider 已有响应式构造器或静态工厂承接。
- Args builder：`*Args` 类名无缺失；当前仅 `PutObjectAPIArgs` 被报告为 minio-java 有 `builder()` 而 reactive 未暴露同等入口。

## 后续动作

`PutObjectAPIArgs` 在 minio-java 中是更底层的 putObject 内部参数组装入口；当前 reactive SDK 用户通常通过 `PutObjectArgs`、`UploadObjectArgs`、`AppendObjectArgs`、`UploadSnowballObjectsArgs` 等高层 Args 调用。下一阶段应判断是否需要为 `PutObjectAPIArgs` 增加公开 builder，还是将其记录为响应式 SDK 有意隐藏的内部边界。

## 边界

- 本阶段不改变公开请求协议。
- 本阶段不引入新依赖。
- 本阶段不降低 Crypto Gate 或破坏性操作门禁。
- 本阶段不写入真实凭证。
