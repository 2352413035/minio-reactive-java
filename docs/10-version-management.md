# 10 Version Management

This repository is maintained as two parallel SDK lines.

## Branches

| Branch/worktree | Java baseline | Purpose |
| --- | --- | --- |
| `master` at `/dxl/minio-project/minio-reactive-java` | JDK 8 | Compatibility line for Java 8 users. Source must avoid Java 9+ language/API features. |
| `chore/jdk17-springboot3` at `/dxl/minio-project/minio-reactive-java-jdk17` | JDK 17+ | Spring Boot 3 / modern Java line. SDK APIs should stay behaviorally aligned with `master`. |

## Workflow

1. Implement and verify changes on `master` first unless the change is JDK17-specific.
2. Mirror source, tests, and docs into `chore/jdk17-springboot3`.
3. Run JDK8 tests on `master`.
4. Run JDK17 tests on `chore/jdk17-springboot3`.
5. Compile the JDK17+ branch with JDK21 and JDK25 when API changes are broad.
6. Commit each branch separately with Lore-style commit messages.
7. Do not push remote branches unless explicitly requested.

## Current local version checkpoints

- JDK8 S3 parity checkpoint: `3bb1b32 Expand reactive SDK parity before full endpoint catalog`
- JDK17+ S3 parity checkpoint: `7303e7e Carry reactive SDK parity onto the JDK17 line`

The full endpoint-catalog tranche should be committed on both branches after validation passes.
