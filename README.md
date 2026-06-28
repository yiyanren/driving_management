# 驾校管理与驾考服务系统

一个基于 Vue 3 + Spring Boot 的前后端分离项目，面向驾校业务场景，覆盖招生线索、学员管理、教学安排、考场信息、报考流程、统计报表等核心功能。

本项目包含两个子模块：

- `web`：前端项目，基于 Vue 3、Vite、Pinia、Vue Router、Ant Design Vue
- `demo`：后端项目，基于 Spring Boot、Spring Security、Spring Data JPA、MySQL、JWT

## 项目功能

- 招生线索管理：线索录入、跟进、转化为学员
- 学员管理：学员信息维护、状态流转、档案管理
- 教学管理：课程计划、训练记录、学时进度
- 考场管理：考场信息维护、Excel 导入、地图展示
- 报考管理：考试报名、状态审核、考场安排
- 报表分析：总览统计、漏斗分析、数据导出
- 用户与权限：登录认证、JWT 鉴权、角色访问控制
- 操作日志：关键业务操作留痕

## 技术栈

### 前端

- Vue 3
- Vite
- Vue Router
- Pinia
- Axios
- Ant Design Vue
- ECharts

### 后端

- Spring Boot
- Spring Security
- Spring Data JPA
- Spring AOP
- JWT
- MySQL
- Apache POI

## 目录结构

```text
drivimg_vue_spring/
|-- web/                     # Vue3 前端
|   |-- src/
|   |   |-- views/          # 页面视图
|   |   |-- stores/         # 状态管理
|   |   |-- router/         # 路由配置
|   |   `-- api.js          # 接口请求封装
|   `-- package.json
|-- demo/                    # Spring Boot 后端
|   |-- src/main/java/com/example/demo/
|   |   |-- controller/     # 控制器层
|   |   |-- service/        # 业务层
|   |   |-- repository/     # 数据访问层
|   |   |-- model/          # 实体模型
|   |   |-- dto/            # 数据传输对象
|   |   |-- security/       # JWT 与认证
|   |   `-- config/         # 配置类
|   |-- src/main/resources/
|   |   `-- application.properties
|   `-- docs/               # 接口与 SQL 文档
|-- 考场信息表.xlsx          # 考场导入样例数据
`-- README.md
```

## 运行环境

- Node.js 18+
- npm 9+
- JDK 17
- Maven 3.9+ 或使用项目自带 Maven Wrapper
- MySQL 8.x

## 快速开始

### 1. 初始化数据库

先创建数据库并导入基础表结构：

```sql
CREATE DATABASE IF NOT EXISTS driving_school DEFAULT CHARACTER SET utf8mb4;
```

你也可以参考后端文档中的 SQL 文件：

- `demo/docs/schema.sql`

### 2. 配置后端数据库连接

编辑文件 `demo/src/main/resources/application.properties`，按你的本地环境修改：

```properties
spring.datasource.url=jdbc:mysql://localhost:3306/driving_school?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai
spring.datasource.username=root
spring.datasource.password=123456
server.port=8080
```

说明：

- 默认后端端口为 `8080`
- `spring.jpa.hibernate.ddl-auto=update` 会根据实体自动更新表结构
- `app.exam-site.local-file-path=../考场信息表.xlsx` 用于导入考场 Excel 数据

### 3. 启动后端

在项目根目录执行：

```bash
cd demo
./mvnw spring-boot:run
```

Windows 也可以使用：

```bash
mvnw.cmd spring-boot:run
```

后端启动后默认地址：

- `http://localhost:8080`

### 4. 启动前端

在另一个终端执行：

```bash
cd web
npm install
npm run dev
```

前端默认地址：

- `http://localhost:5173`

前端开发服务器已将 `/api` 代理到：

- `http://localhost:8080`

## 默认账号

后端接口文档中给出了可直接登录的默认账号：

- 管理员：`admin / admin123`
- 招生：`sales / sales123`
- 教练：`coach / coach123`
- 学员：`student / student123`

## 主要接口

接口统一以前缀 `/api` 暴露，除登录外，大部分接口需要在请求头中携带：

```http
Authorization: Bearer <token>
```

主要模块包括：

- `/api/auth`：登录、注册
- `/api/leads`：招生线索管理
- `/api/students`：学员管理
- `/api/teaching`：教学计划与训练记录
- `/api/exam-sites`：考场管理与导入
- `/api/exam-applications`：考试报名与审核
- `/api/reports`：统计报表与导出

详细接口说明见：

- `demo/docs/api.md`

## 前端页面

前端已实现多个业务页面，包括但不限于：

- 登录页
- 仪表盘
- 招生管理
- 学员管理
- 教练管理
- 教学管理
- 考试与预约
- 地图展示
- 在线学习
- 统计报表
- 个人中心

## 地图配置

项目中包含高德地图加载逻辑。如果你需要启用地图功能，请在前端侧配置高德 Key。

参考 `web/README.md` 中的说明，在前端目录创建本地环境变量文件并添加：

```bash
VITE_AMAP_KEY=你的高德Web端Key
```

## 文档说明

- `demo/docs/api.md`：后端接口说明
- `demo/docs/schema.sql`：数据库初始化 SQL
- `demo/docs/demo.http`：接口调试示例
- `web/README.md`：前端单独启动说明

## 注意事项

- 当前配置中的数据库账号密码为本地开发示例，实际使用前请自行修改
- JWT 密钥目前写在配置文件中，正式环境应改为更安全的注入方式
- `application.properties` 中的第三方 API Key 建议替换为你自己的有效配置
- 若首次导入考场数据，请确认 `考场信息表.xlsx` 路径存在且可访问

## 适用场景

适合作为以下用途的课程设计或练手项目：

- Java Web 前后端分离实训
- Spring Boot + Vue 综合项目作业
- 驾校管理业务系统原型
- 信息管理系统课程设计
