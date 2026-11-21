# 艺术培训管理系统 - 前端

基于 Vue 3 + Vite + Element Plus 的前端应用，为艺术培训管理系统提供学员端、教师端和运营管理端的用户界面。

## 技术栈

- Vue 3
- Vue Router 4
- Pinia
- Axios
- Element Plus
- Vite

## 目录结构

frontend/
├── public/
├── src/
│ ├── api/ # API 请求封装
│ │ ├── request.js # Axios 实例与拦截器
│ │ ├── auth.js
│ │ ├── admin.js
│ │ ├── teacher.js
│ │ └── student.js
│ ├── assets/
│ │ └── styles/
│ │ └── main.css # 全局样式
│ ├── components/ # 公共组件
│ │ ├── PageHeader.vue
│ │ └── Pagination.vue
│ ├── layouts/ # 布局组件
│ │ ├── AdminLayout.vue
│ │ ├── TeacherLayout.vue
│ │ └── StudentLayout.vue
│ ├── router/
│ │ └── index.js # 路由配置与守卫
│ ├── store/
│ │ ├── user.js # 用户状态
│ │ └── app.js # 应用状态（可选）
│ ├── views/
│ │ ├── Login.vue
│ │ ├── admin/ # 管理端页面
│ │ ├── teacher/ # 教师端页面
│ │ └── student/ # 学员端页面
│ ├── App.vue
│ └── main.js
├── index.html
├── package.json
├── vite.config.js
└── README.md

## 快速开始

### 1. 安装依赖

```bash
npm install
```

### 2. 启动开发服务器

```bash
npm run dev
```

默认访问地址：http://localhost:5173

### 3. 构建生产版本

```bash
npm run build
```

## 环境说明

后端 API 默认代理到 http://localhost:8080（见 vite.config.js）。

登录账号请使用后端初始化数据中的测试账号，例如 admin / 123456（需确保后端数据库已初始化）。

## 主要功能

- 三端分离：学员端、教师端、运营管理端
- 登录认证与基于角色的路由守卫
- 统一的 Axios 拦截器（自动携带 Token、错误提示）
- Element Plus 组件库与图标
