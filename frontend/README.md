# 安全云盘系统 - 前端

基于 Vue 3 + Vite + Element Plus 的前端项目

## 技术栈

- **Vue 3** - 渐进式JavaScript框架
- **Vite** - 下一代前端构建工具
- **Element Plus** - Vue 3 UI组件库
- **Vue Router** - 官方路由管理器
- **Pinia** - Vue 3 状态管理库
- **Axios** - HTTP客户端

## 项目结构

```
frontend/
├── src/
│   ├── api/           # API接口
│   ├── assets/        # 静态资源
│   ├── components/    # 公共组件
│   ├── router/        # 路由配置
│   ├── store/         # 状态管理
│   ├── utils/         # 工具函数
│   ├── views/         # 页面组件
│   ├── App.vue        # 根组件
│   └── main.js        # 入口文件
├── index.html         # HTML模板
├── package.json       # 项目配置
└── vite.config.js     # Vite配置
```

## 快速开始

### 1. 安装依赖

```bash
cd frontend
npm install
```

### 2. 启动开发服务器

```bash
npm run dev
```

前端服务将在 http://localhost:3000 启动

### 3. 构建生产版本

```bash
npm run build
```

## 已完成功能

### ✅ 第一轮：用户认证模块

- [x] 用户注册页面
- [x] 用户登录页面
- [x] 邮箱验证码发送
- [x] JWT Token 认证
- [x] 路由守卫
- [x] 状态管理

## 接口说明

### 后端接口地址

开发环境：http://localhost:8080/api

已配置代理，前端请求 `/api` 会自动转发到后端服务器

### 主要接口

- `POST /api/auth/send-code` - 发送验证码
- `POST /api/auth/register` - 用户注册
- `POST /api/auth/login` - 用户登录

## 开发注意事项

1. 确保后端服务已启动（端口8080）
2. 首次使用需要先注册账号
3. 登录需要邮箱验证码（验证码发送到注册邮箱）

## 下一步开发计划

### 第二轮：文件上传模块（待开发）

- [ ] 文件上传组件
- [ ] 上传进度显示
- [ ] 分片上传
- [ ] 文件加密

---

**技术支持：** Claude Code
