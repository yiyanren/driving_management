<script setup>
import { onMounted, reactive, ref } from "vue";
import { message } from "ant-design-vue";
import { api } from "../api";

const loading = ref(false);
const list = ref([]);
const total = ref(0);
const query = reactive({ page: 0, size: 10, keyword: "", status: "" });
const editVisible = ref(false);
const editLoading = ref(false);
const editForm = reactive({ id: null, name: "", phone: "", idCard: "", status: "" });

const columns = [
  { title: "ID", dataIndex: "id", key: "id", width: 80 },
  { title: "姓名", dataIndex: "name", key: "name" },
  { title: "手机号", dataIndex: "phone", key: "phone" },
  { title: "身份证", dataIndex: "idCard", key: "idCard" },
  { title: "状态", dataIndex: "status", key: "status" },
  { title: "操作", key: "action", width: 180 }
];

const loadData = async () => {
  loading.value = true;
  try {
    const res = await api.listStudents(query);
    if (res.data.success) {
      list.value = res.data.data.content;
      total.value = res.data.data.totalElements;
    }
  } finally {
    loading.value = false;
  }
};

const openEdit = (row) => {
  editForm.id = row.id;
  editForm.name = row.name;
  editForm.phone = row.phone;
  editForm.idCard = row.idCard;
  editForm.status = row.status;
  editVisible.value = true;
};

const submitEdit = async () => {
  if (!editForm.id) return;
  editLoading.value = true;
  try {
    const payload = {
      name: editForm.name,
      phone: editForm.phone,
      idCard: editForm.idCard,
      status: editForm.status
    };
    const res = await api.updateStudent(editForm.id, payload);
    if (!res.data.success) return message.error(res.data.message || "更新失败");
    message.success("学员信息更新成功");
    editVisible.value = false;
    loadData();
  } finally {
    editLoading.value = false;
  }
};

const removeStudent = async (row) => {
  const res = await api.deleteStudent(row.id);
  if (!res.data.success) return message.error(res.data.message || "删除失败");
  message.success("学员删除成功");
  loadData();
};

onMounted(loadData);
</script>

<template>
  <a-space direction="vertical" style="display:flex;" :size="16">
    <a-card title="查询条件">
      <a-row :gutter="12">
        <a-col :xs="24" :sm="12" :lg="8"><a-input v-model:value="query.keyword" placeholder="姓名/手机号/身份证" /></a-col>
        <a-col :xs="24" :sm="12" :lg="8"><a-input v-model:value="query.status" placeholder="状态" /></a-col>
        <a-col :xs="24" :sm="12" :lg="8"><a-button type="primary" @click="query.page=0;loadData()">查询</a-button></a-col>
      </a-row>
    </a-card>
    <a-card title="学员列表">
      <a-table
        :loading="loading"
        :columns="columns"
        :data-source="list"
        row-key="id"
        :pagination="{ current: query.page + 1, pageSize: query.size, total, onChange: (p,s)=>{ query.page=p-1; query.size=s; loadData(); } }"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'action'">
            <a-space>
              <a-button size="small" style="background:#e6f4ff;border-color:#91caff;color:#0958d9;" @click="openEdit(record)">编辑</a-button>
              <a-popconfirm title="确认删除该学员吗？" ok-text="确认" cancel-text="取消" @confirm="removeStudent(record)">
                <a-button size="small" style="background:#fff1f0;border-color:#ffa39e;color:#cf1322;">删除</a-button>
              </a-popconfirm>
            </a-space>
          </template>
        </template>
      </a-table>
    </a-card>
    <a-modal v-model:open="editVisible" title="编辑学员" :confirm-loading="editLoading" @ok="submitEdit">
      <a-space direction="vertical" style="display:flex;">
        <a-input v-model:value="editForm.name" placeholder="姓名" />
        <a-input v-model:value="editForm.phone" placeholder="手机号" />
        <a-input v-model:value="editForm.idCard" placeholder="身份证号" />
        <a-input v-model:value="editForm.status" placeholder="状态" />
      </a-space>
    </a-modal>
  </a-space>
</template>
