<script setup>
import { onMounted, reactive, ref } from "vue";
import { message } from "ant-design-vue";
import { api } from "../api";

const loading = ref(false);
const list = ref([]);
const total = ref(0);
const query = reactive({ page: 0, size: 10, keyword: "", status: "" });
const leadForm = reactive({ name: "", phone: "", source: "", status: "新建", owner: "" });

const columns = [
  { title: "姓名", dataIndex: "name", key: "name" },
  { title: "手机号", dataIndex: "phone", key: "phone" },
  { title: "来源", dataIndex: "source", key: "source" },
  { title: "状态", dataIndex: "status", key: "status" },
  { title: "招生人员", dataIndex: "owner", key: "owner" }
];

const loadData = async () => {
  loading.value = true;
  try {
    const res = await api.listLeads(query);
    if (res.data.success) {
      list.value = res.data.data.content;
      total.value = res.data.data.totalElements;
    }
  } finally {
    loading.value = false;
  }
};

const createLead = async () => {
  const res = await api.createLead({ ...leadForm });
  if (!res.data.success) return message.error(res.data.message || "创建失败");
  message.success("创建成功");
  Object.assign(leadForm, { name: "", phone: "", source: "", status: "新建", owner: "" });
  loadData();
};

onMounted(loadData);
</script>

<template>
  <a-space direction="vertical" style="display:flex;" :size="16">
    <a-card title="查询条件">
      <a-row :gutter="12">
        <a-col :xs="24" :sm="12" :lg="8"><a-input v-model:value="query.keyword" placeholder="姓名/手机号/来源" /></a-col>
        <a-col :xs="24" :sm="12" :lg="8"><a-input v-model:value="query.status" placeholder="状态：新建/跟进/转化/失效" /></a-col>
        <a-col :xs="24" :sm="12" :lg="8"><a-button type="primary" @click="query.page=0;loadData()">查询</a-button></a-col>
      </a-row>
    </a-card>
    <a-card title="意向客户">
      <a-row :gutter="12">
        <a-col :xs="24" :sm="12" :lg="6"><a-input v-model:value="leadForm.name" placeholder="姓名" /></a-col>
        <a-col :xs="24" :sm="12" :lg="6"><a-input v-model:value="leadForm.phone" placeholder="手机号" /></a-col>
        <a-col :xs="24" :sm="12" :lg="6"><a-input v-model:value="leadForm.source" placeholder="来源" /></a-col>
        <a-col :xs="24" :sm="12" :lg="6"><a-input v-model:value="leadForm.owner" placeholder="招生人员" /></a-col>
      </a-row>
      <a-button style="margin-top:12px;" type="primary" @click="createLead">新增意向客户</a-button>
    </a-card>
    <a-card title="意向客户列表">
      <a-table
        :loading="loading"
        :columns="columns"
        :data-source="list"
        row-key="id"
        :pagination="{ current: query.page + 1, pageSize: query.size, total, onChange: (p,s)=>{ query.page=p-1; query.size=s; loadData(); } }"
      />
    </a-card>
  </a-space>
</template>
