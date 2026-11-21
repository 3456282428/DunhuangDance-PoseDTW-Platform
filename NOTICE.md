第三方资源与许可声明（NOTICE）
==================================

本项目在研发与运行中使用了以下第三方资源。各组件的权利与许可归其各自权利人所有，
本仓库仅按其原有许可使用，不改变也不扩大其许可范围。
完整清单与用途说明见 `docs/03-第三方资源与许可.md`。

一、开源软件与框架
Spring Boot 3.2.5 (Apache-2.0)、Spring Security (Apache-2.0)、MyBatis 3 (Apache-2.0)、
PageHelper (MIT)、jjwt (Apache-2.0)、jieba-analysis (MIT)、Lombok (MIT)、
MySQL Connector/J (GPL-2.0 with FOSS Exception)、
Vue 3 (MIT)、Vue Router (MIT)、Pinia (MIT)、Element Plus / Icons (MIT)、
ECharts (Apache-2.0)、axios (MIT)、Vite (MIT)、
FastAPI (MIT)、Uvicorn (BSD-3-Clause)、MediaPipe (Apache-2.0)、OpenCV (Apache-2.0)、
NumPy / SciPy / scikit-learn (BSD-3-Clause)、librosa (ISC)、Matplotlib (PSF 风格)、
Pillow (HPND)、python-multipart (Apache-2.0)。

二、数据集（仅学术研究与教学演示，本仓库不分发原始文件）
- FineDance（ICCV 2023，CC BY 4.0）：动作自编码器训练与敦煌舞序列精调。
- AIST++（Google Research，CC BY 4.0；上游 AIST Dance Video Database 受官网 Terms of Use 约束）：跨舞种评分基准。

三、人工智能模型与服务（需使用者自备密钥，遵守各平台协议）
- 豆包 doubao-seed-2-0-mini（火山引擎方舟 Ark）：关键帧语义理解与教学点评。
- 豆包 doubao-seedance-video（火山引擎方舟 Ark）：教学视频生成，默认未启用。
- DeepSeek Chat（OpenAI 兼容接口）：知识库问答与智能体推理。

四、素材、字体与图标
- 敦煌壁画装饰图样：取自公开敦煌壁画数字资源，仅用于界面装饰与演示，本仓库未包含大体积装饰图。
- 字体：使用操作系统自带字体，仓库未嵌入或分发字体文件。
- 图标：Element Plus Icons（MIT）与项目内自绘 SVG。

五、合规声明
1. 本项目仅用于学习、研究与教学演示，不用于商业销售或其他商业用途。
2. 数据集仅用于学术研究与算法验证，不分发原始文件。
3. 第三方商业模型服务由使用者自行申请密钥，本项目不提供、不共享任何密钥。
4. 展示素材中的音乐、壁画图样等如权利人提出异议，将立即下架或替换。

六、引用
- Li R, Zhao J, Zhang Y, et al. FineDance: A Fine-grained Choreography Dataset for 3D Full Body Dance Generation. ICCV, 2023.
- Li R, Yang S, Ross D A, et al. Learn to Dance with AIST++: Music Conditioned 3D Dance Generation. arXiv:2101.08779, 2021.
- Bazarevsky V, et al. MediaPipe Pose: 实时人体姿态估计. Google Research.
