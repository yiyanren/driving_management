# 驾考一点通后端 API（第二版）

## 通用
- Base URL: `/api`
- 统一返回:
```json
{ "success": true, "message": "OK", "data": {} }
```
- 除登录外全部接口要求 `Authorization: Bearer <token>`
- 角色：`管理员` / `招生` / `教练` / `学员`

## 认证
- `POST /auth/login` 登录
- 默认账号：
- `admin/admin123`（管理员）
- `sales/sales123`（招生）
- `coach/coach123`（教练）
- `student/student123`（学员）

## 招生（管理员/招生）
- `GET /leads?page=0&size=10&keyword=&status=` 分页查询线索
- `GET /leads/{id}` 线索详情
- `POST /leads` 新增线索
- `PUT /leads/{id}` 更新线索
- `PATCH /leads/{id}/status?status=跟进中` 状态流转
- `POST /leads/{id}/convert?idCard=xxxx` 转学员
- `DELETE /leads/{id}` 删除线索

## 学员
- `GET /students?page=0&size=10&keyword=&status=` 分页查询
- `GET /students/{id}` 详情
- `POST /students` 新增（管理员/招生）
- `PUT /students/{id}` 更新（管理员/招生）
- `PATCH /students/{id}/status?status=培训中` 状态流转（管理员/招生/教练）
- `DELETE /students/{id}` 删除（管理员）

## 教学（管理员/教练）
- `GET /teaching/plans?page=0&size=10&studentId=1` 课程计划分页
- `POST /teaching/plans` 新建课程计划
- `GET /teaching/records?page=0&size=10&studentId=1` 训练记录分页
- `POST /teaching/records` 新建训练记录
- `GET /teaching/progress?studentId=1` 学时进度

## 考场与报考
- `GET /exam-sites?page=0&size=10&keyword=` 考场分页
- `GET /exam-sites/{id}` 详情
- `POST /exam-sites` 新建考场（支持容量/场次）
- `PUT /exam-sites/{id}` 更新考场
- `DELETE /exam-sites/{id}` 删除考场
- `GET /exam-applications?page=0&size=10&studentId=&status=` 报考分页
- `GET /exam-applications/{id}` 报考详情
- `POST /exam-applications` 提交报考（含容量校验）
- `PATCH /exam-applications/{id}/status?status=已通过` 报考审核

## 报表（管理员/招生）
- `GET /reports/overview?from=2026-01-01&to=2026-12-31` 总览
- `GET /reports/funnel?from=2026-01-01&to=2026-12-31` 漏斗
- `GET /reports/export/overview?from=&to=` 导出 CSV
