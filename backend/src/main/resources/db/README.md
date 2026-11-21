# 数据库脚本说明（权威基线）

## 全新部署（唯一推荐流程）
1. 执行 `init.sql`：建库 `art_training`、25 张基表、视图、触发器、全部外键/CHECK/唯一约束。
2. 执行 `seed_clean.sql`：干净演示数据（账号密码均为 123456）。

应用启动时**不会**自动执行任何 SQL（未配置 spring.sql.init）。

## 文件角色
| 文件 | 角色 |
|---|---|
| `init.sql` | **权威建表脚本**，与运行库结构保持一致，结构变更必须同步本文件 |
| `seed_clean.sql` | **权威干净种子**，清空重建时使用 |
| `schema_live.sql` | 运行库 `SHOW CREATE` 结构快照（仅结构，便于对照），每次结构变更后重新导出 |
| `migrate_*.sql` | 历次优化的增量迁移记录，只用于旧库升级，不在全新部署中执行 |
| `seed_clean.py` | 生成/校验种子的辅助脚本 |
| `archive/` | 已废弃的旧脚本（旧 data.sql、extra_tables.sql），禁止用于当前版本 |

## 结构变更纪律
任何对运行库的结构修改，必须同时更新 `init.sql` 并重新导出 `schema_live.sql`，
保证「运行库 = init.sql 重建结果 = schema_live.sql 快照」三者一致。
