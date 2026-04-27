# 驾考一点通前端（Vue3 + Vite）

## 启动步骤

1. 进入目录

```bash
cd web
```

2. 安装依赖

```bash
npm install
```

3. 启动开发环境

```bash
npm run dev
```

默认访问 `http://localhost:5173`。

## 说明

- 开发服务器已代理 `/api` 到 `http://localhost:8080`
- 依赖后端接口在 `demo` 模块中

## 高德地图配置

1. 复制环境变量模板

```bash
cp .env.example .env.local
```

2. 在 `.env.local` 中配置你的高德 Web Key

```bash
VITE_AMAP_KEY=你的高德Web端Key
```

3. 重启前端服务后生效

- 地图页面使用高德 JS API 2.0，实现考场展示与驾车路线规划。
