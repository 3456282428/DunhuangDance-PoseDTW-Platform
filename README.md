# 舞韵敦煌 · DunhuangDance-PoseDTW-Platform

基于**姿态估计 + 动态时间规整（DTW）**的敦煌舞蹈 AI 辅助教学与评分平台。系统由管理端、教师端、学员端三端业务系统与 AI 评分引擎组成：业务侧覆盖课程排课、报名缴费、考勤、作业、考级比赛、调课审批与运营统计；教学侧面向敦煌舞场景，提供师生视频自动评分、分部位诊断、节奏对齐分析、骨架叠加对比与 AI 语义点评。

> 本项目为个人学习与毕业设计性质的开源工程，**仅限学习、研究与教学演示使用，禁止任何商业用途**。详见 [LICENSE](./LICENSE)。

## 核心能力

- **双通道评分**：通道一用 MediaPipe Pose 提取 33 关键点 → 关节角度序列 → DTW 时序对齐 → 6 部位 / 3 维度量化评分 → Ridge 回归校准；通道二抽取关键帧送入视觉大模型，在量化结果约束下生成教学语言点评，语义通道不修改分数。
- **节奏对齐**：抽取音轨跟踪音乐节拍，用关键点位移能量峰值检测动作节拍，在 ±150 ms 容差内计算命中率、平均偏差与速度一致性，并做全局偏移搜索 + 10 秒窗口分段相位精化。
- **敦煌动作资产**：以公开舞蹈数据集的派生动作序列训练 MLP 自编码器，并对敦煌舞序列做定向精调，支撑八类标志性动作的骨架回放与关键姿态展示。
- **三端知识库与智能体**：知识库按管理端 / 教师端 / 学员端隔离存储，Agent 以工具调用方式执行创建课程、调课申请与审批等真实业务操作，敏感操作二次确认。
- **完整教务业务链**：课程、班级、排课冲突检测、报名缴费、考勤、作业批改、考级比赛、调课审批、财务与运营统计。

## 目录结构

```
backend/                     Spring Boot 3 + MyBatis 后端
  src/main/java/com/art/     控制器 / 服务 / Mapper / 智能体工具
  src/main/resources/
    application.yml          主配置（密钥全部走环境变量）
    application-dev.yml      开发环境数据源配置
    db/                      建表、迁移与演示数据 SQL
frontend/                    Vue 3 + Vite 三端前端（src / public / 配置）
dance/                       Python FastAPI 评分服务
  api_server.py              评分服务入口（默认 8000 端口）
  modules/                   预处理、姿态提取、DTW、节奏、评分、可视化
  models/                    评分校准模型与敦煌参考动作库（pkl）
docs/                        作品介绍、运行说明、第三方资源与 AI 工具说明
scripts/                     一键启动 / 停止脚本（Windows）
```

## 快速开始

环境要求：JDK 17+、MySQL 8、Node.js 18+、Python 3.10、FFmpeg。

1. **导入数据库**：先执行 `backend/src/main/resources/db/schema_live.sql`（或 `init.sql`）建库建表，再按需导入 `seed_dunhuang.sql` / `seed_clean.sql` 演示数据。
2. **启动后端（8080）**
   ```bash
   cd backend
   mvn spring-boot:run
   ```
3. **启动评分服务（8000）**
   ```bash
   cd dance
   pip install -r requirements.txt
   python api_server.py
   ```
4. **启动前端（5173）**
   ```bash
   cd frontend
   npm install
   npm run dev
   ```
5. 浏览器访问 `http://localhost:5173`。

Windows 下也可以直接执行 `scripts/start-system.bat` 一次拉起三个服务（需要预先 `mvn package` 生成后端 jar）。

演示账号（**仅本地演示，务必自行修改**）：管理员 `admin`、教师 `T001`、学员 `20240102`，初始密码均为 `123456`。

## 配置与密钥

仓库中不含任何真实密钥，以下项目需自行注入：

| 配置项 | 方式 | 说明 |
|---|---|---|
| `JWT_SECRET` | 环境变量 | JWT 签名密钥，未设置时使用仅供调试的占位值 |
| `DB_URL` / `DB_USERNAME` / `DB_PASSWORD` | 环境变量 | 数据库连接，默认 `localhost:3306/art_training` |
| `DANCE_API_URL` | 环境变量 | 评分服务地址，默认 `http://127.0.0.1:8000` |
| 大模型 API Key | 管理端「AI 模型管理」页面填写 | 豆包视觉模型 / DeepSeek，量化评分不依赖密钥 |

详细部署与验收步骤见 [docs/02-运行与使用说明.md](./docs/02-运行与使用说明.md)，功能与算法说明见 [docs/01-作品介绍.md](./docs/01-作品介绍.md)。

## 开源范围说明

本仓库是**代码与文档的精简开源版本**，出于仓库体积与第三方素材授权考虑，未包含以下内容：

- 作品展示视频、操作录屏、答辩 PPT、设计说明书 PDF 与页面截图（合计超过 1 GB）；
- 前端预构建产物 `dist/` 与依赖 `node_modules/`、后端编译产物 `target/`；
- 数据集派生的大体积样例（`keypoints3d.zip`、`motions.zip`、示例 mp4）；
- 装饰用敦煌壁画大图（`frontend/public` 中的 banner 图），仅保留图标与头像等小体积资源；
- 运行时用户数据快照（`uploads/` 目录与线上数据库全量导出）。

如需完整展示材料，请参考项目交付包。

## 第三方资源与许可

系统依赖的框架、数据集与大模型服务清单见 [docs/03-第三方资源与许可.md](./docs/03-第三方资源与许可.md)。数据集（FineDance、AIST++ 等）仅用于学术研究与教学演示，本仓库不分发其原始文件。界面装饰图样、展示视频音乐等素材版权归各自权利人，如权利人提出异议将立即删除或替换。

## 免责声明

本项目按「现状」提供，不附带任何明示或暗示的担保。作者不主张对第三方素材、数据集、模型服务的任何权利，也不对使用本代码产生的任何后果承担责任。评分结果仅作为教学辅助参考，不替代教师评价。
