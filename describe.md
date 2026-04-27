# 驾考一点通（Vue + Spring Boot）实施说明

## 1. 模块目标
- 招生模块：线索录入、状态流转（新建/跟进/转化/失效）、来源统计、招生人员业绩。
- 教学模块：学员档案、课程计划、教练分配、训练记录、学时进度。
- 报考模块：科目报考申请、考场信息维护（地址/场次/容量）、报考审核、状态追踪。
- 路线图模块：考场地理位置展示、推荐路线（当前为地图容器占位，预留第三方地图 API）。
- 报表模块：招生转化报表、教学完成率报表、报考通过率报表、时间维度筛选、CSV 导出。

## 2. 后端实现（Spring Boot）
### 2.1 架构
- 分层：`controller / service / repository / model`，统一异常与返回结构。
- 认证授权：`JWT + RBAC`，角色为 `管理员/招生/教练/学员`。
- 返回结构：`ApiResponse<T>`，分页结果：`PageResult<T>`。
- 非功能：分页与条件查询、操作日志（AOP 注解 `@OpLog`）、审计时间字段。

### 2.2 核心实体
- `User`：系统用户（用户名、密码、角色、启用状态）。
- `Lead`：线索。
- `Student`：学员。
- `CoursePlan`：课程计划。
- `TrainingRecord`：训练记录。
- `ExamSite`：考场（新增 `sessionName/capacity/reservedCount`）。
- `ExamApplication`：报考申请。
- `OperationLog`：操作日志。

### 2.3 核心能力
- 登录：`/api/auth/login`，系统启动自动初始化 4 个演示账号。
- 权限：接口级 `@PreAuthorize` 按角色控制。
- 招生：线索分页筛选、线索转学员、来源统计、人员业绩统计。
- 教学：课程计划、训练记录、学时进度（`/api/teaching/progress`）。
- 报考：报考分页、审核、考场容量校验（满员拒绝报考）。
- 报表：总览、漏斗、CSV 导出（`/api/reports/export/overview`）。

## 3. 前端实现（Vue）
### 3.1 工程化
- 路由：`vue-router`，页面级权限守卫。
- 状态管理：`pinia`，统一维护 token 与用户信息。
- 请求层：`axios` 拦截器注入 token，401 失效重登，403 仅提示无权限。

### 3.2 页面结构
- 登录页：`LoginView`（按角色自动跳转默认首页）。
- 主框架：`MainLayout`（按角色动态菜单）。
- 业务页：`Dashboard/Leads/Students/Teaching/Exam/Map/Reports`。

### 3.3 体验策略
- 列表页统一分页查询（关键词、状态等）。
- 报表页支持时间筛选与一键导出 CSV。
- 地图页已预留地图容器，后续可直接接入高德/百度 SDK。

## 4. 默认账号
- 管理员：`admin / admin123`
- 招生：`sales / sales123`
- 教练：`coach / coach123`
- 学员：`student / student123`

## 5. 后续建议
- 补充按钮级权限指令（如 `v-permission`）。
- 报表导出增加 Excel（xlsx）格式。
- 地图模块接入真实路径规划 API（驾车/公交/打车时长对比）。
- 增加自动化测试（service 层与接口权限回归）。

