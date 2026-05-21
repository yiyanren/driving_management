<script setup>
import { onMounted, reactive, ref } from "vue";
import { message } from "ant-design-vue";
import { api } from "../api";

const loading = ref(false);
const list = ref([]);
const total = ref(0);
const query = reactive({ page: 0, size: 10, role: "教练" });

const createVisible = ref(false);
const createLoading = ref(false);
const createForm = reactive({ username: "", password: "", displayName: "", role: "教练", drivingSchoolId: undefined });
const schools = ref([]);

const editVisible = ref(false);
const editLoading = ref(false);
const editForm = reactive({ id: null, displayName: "", password: "", enabled: true, drivingSchoolId: undefined });

const columns = [
  { title: "账号", dataIndex: "username", key: "username" },
  { title: "教练姓名", dataIndex: "displayName", key: "displayName" },
  { title: "所属驾校", dataIndex: "schoolName", key: "schoolName" },
  { title: "状态", dataIndex: "enabled", key: "enabled" },
  { title: "操作", key: "action", width: 180 }
];

const loadData = async () => {
  loading.value = true;
  try {
    const res = await api.listUsers(query);
    if (res.data.success) {
      const users = res.data.data.content;
      
      // 先将基础数据转换为带 schoolName 的形式
      const mappedUsers = users.map(user => {
        const school = schools.value.find(s => s.id === user.drivingSchoolId);
        return {
          ...user,
          schoolName: school ? school.name : '未分配'
        };
      });
      
      // 在前端进行分组排序：先按 drivingSchoolId 排序，再按 createdAt（或 id）排序
      mappedUsers.sort((a, b) => {
        const schoolA = a.drivingSchoolId || 0;
        const schoolB = b.drivingSchoolId || 0;
        if (schoolA !== schoolB) {
          return schoolA - schoolB; // 按驾校 ID 分类聚拢
        }
        // 驾校相同时，按时间（这里使用 ID 作为时间的代理，因为 ID 是自增的）倒序
        return b.id - a.id; 
      });
      
      list.value = mappedUsers;
      total.value = res.data.data.totalElements;
    }
  } finally {
    loading.value = false;
  }
};

const openCreate = () => {
  createForm.username = "";
  createForm.password = "";
  createForm.displayName = "";
  createForm.schoolId = undefined;
  createVisible.value = true;
};

const submitCreate = async () => {
  if (!createForm.username || !createForm.password) return message.warning("用户名和密码不能为空");
  createLoading.value = true;
  try {
    const res = await api.createUser(createForm);
    if (!res.data.success) return message.error(res.data.message || "创建失败");
    message.success("教练账号创建成功");
    createVisible.value = false;
    loadData();
  } catch (e) {
    message.error(e?.response?.data?.message || "创建失败，用户名可能已存在");
  } finally {
    createLoading.value = false;
  }
};

const openEdit = (row) => {
  editForm.id = row.id;
  editForm.displayName = row.displayName;
  editForm.drivingSchoolId = row.drivingSchoolId;
  editForm.password = ""; // 密码留空表示不修改
  editForm.enabled = row.enabled;
  editVisible.value = true;
};

const submitEdit = async () => {
  if (!editForm.id) return;
  editLoading.value = true;
  try {
    const payload = {
      displayName: editForm.displayName,
      enabled: editForm.enabled,
      drivingSchoolId: editForm.drivingSchoolId
    };
    if (editForm.password) {
      payload.password = editForm.password;
    }
    const res = await api.updateUser(editForm.id, payload);
    if (!res.data.success) return message.error(res.data.message || "更新失败");
    message.success("教练信息更新成功");
    editVisible.value = false;
    loadData();
  } finally {
    editLoading.value = false;
  }
};

const removeCoach = async (row) => {
  const res = await api.deleteUser(row.id);
  if (!res.data.success) return message.error(res.data.message || "删除失败");
  message.success("教练删除成功");
  loadData();
};

onMounted(async () => {
  try {
    const res = await api.listAllDrivingSchools();
    if(res.data.success) {
      schools.value = res.data.data || [];
    }
  } catch(e) {}
  loadData();
});
</script>

<template>
  <a-space direction="vertical" style="display:flex;" :size="16">
    <a-card title="教练管理">
      <a-row :gutter="12" style="margin-bottom: 16px;">
        <a-col :xs="24" :sm="12" :lg="8">
          <a-select 
            v-model:value="query.drivingSchoolId" 
            placeholder="按驾校归类筛选" 
            style="width: 100%"
            allowClear
            :options="schools.map(s => ({ label: s.name, value: s.id }))"
            @change="query.page=0; loadData()"
          />
        </a-col>
      </a-row>
      <template #extra>
        <a-button type="primary" @click="openCreate">新增教练</a-button>
      </template>
      <a-table
        :loading="loading"
        :columns="columns"
        :data-source="list"
        row-key="id"
        :pagination="{ current: query.page + 1, pageSize: query.size, total, onChange: (p,s)=>{ query.page=p-1; query.size=s; loadData(); } }"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'enabled'">
            <a-tag :color="record.enabled ? 'green' : 'red'">{{ record.enabled ? '正常' : '禁用' }}</a-tag>
          </template>
          <template v-if="column.key === 'action'">
            <a-space>
              <a-button size="small" style="background:#e6f4ff;border-color:#91caff;color:#0958d9;" @click="openEdit(record)">编辑</a-button>
              <a-popconfirm title="确认删除该教练账号吗？" ok-text="确认" cancel-text="取消" @confirm="removeCoach(record)">
                <a-button size="small" style="background:#fff1f0;border-color:#ffa39e;color:#cf1322;">删除</a-button>
              </a-popconfirm>
            </a-space>
          </template>
        </template>
      </a-table>
    </a-card>

    <a-modal v-model:open="createVisible" title="新增教练" :confirm-loading="createLoading" @ok="submitCreate">
      <a-form layout="vertical">
        <a-form-item label="登录账号 (英文字母)" required>
          <a-input v-model:value="createForm.username" placeholder="请输入账号" />
        </a-form-item>
        <a-form-item label="初始密码" required>
          <a-input-password v-model:value="createForm.password" placeholder="请输入密码" />
        </a-form-item>
        <a-form-item label="教练真实姓名" required>
          <a-input v-model:value="createForm.displayName" placeholder="请输入教练姓名" />
        </a-form-item>
        <a-form-item label="所属驾校">
          <a-select 
            v-model:value="createForm.drivingSchoolId" 
            placeholder="请选择所属驾校" 
            style="width: 100%"
            show-search
            option-filter-prop="label"
            :options="schools.map(s => ({ label: s.name, value: s.id }))"
            allowClear
          />
        </a-form-item>
      </a-form>
    </a-modal>

    <a-modal v-model:open="editVisible" title="编辑教练" :confirm-loading="editLoading" @ok="submitEdit">
      <a-form layout="vertical">
        <a-form-item label="教练姓名">
          <a-input v-model:value="editForm.displayName" placeholder="例如：张教练" />
        </a-form-item>
        <a-form-item label="重置密码">
          <a-input-password v-model:value="editForm.password" placeholder="不修改请留空" />
        </a-form-item>
        <a-form-item label="所属驾校">
          <a-select 
            v-model:value="editForm.drivingSchoolId" 
            placeholder="请选择所属驾校" 
            style="width: 100%"
            show-search
            option-filter-prop="label"
            :options="schools.map(s => ({ label: s.name, value: s.id }))"
            allowClear
          />
        </a-form-item>
        <a-form-item label="账号状态">
          <a-switch v-model:checked="editForm.enabled" checked-children="正常" un-checked-children="禁用" />
        </a-form-item>
      </a-form>
    </a-modal>
  </a-space>
</template>
