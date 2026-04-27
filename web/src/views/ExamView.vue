<script setup>
import { computed, onMounted, reactive, ref } from "vue";
import { message } from "ant-design-vue";
import { api } from "../api";

const siteForm = reactive({
  name: "",
  address: "",
  latitude: "",
  longitude: "",
  routeGuide: "",
  sessionName: "",
  capacity: 30
});
const examForm = reactive({ studentId: undefined, subjectCode: "K1", examSiteId: "", examDate: "", status: "已申请" });
const sites = ref([]);
const apps = ref([]);
const students = ref([]);

const siteColumns = [
  { title: "ID", dataIndex: "id", key: "id", width: 72 },
  { title: "考场", dataIndex: "name", key: "name" },
  { title: "场次", dataIndex: "sessionName", key: "sessionName" },
  { title: "容量", dataIndex: "capacity", key: "capacity" },
  { title: "已约", dataIndex: "reservedCount", key: "reservedCount" },
  { title: "地址", dataIndex: "address", key: "address" }
];

const appColumns = [
  { title: "ID", dataIndex: "id", key: "id", width: 72 },
  { title: "学员", dataIndex: "studentDisplay", key: "studentDisplay" },
  { title: "科目", dataIndex: "subjectCode", key: "subjectCode" },
  { title: "考场ID", dataIndex: "examSiteId", key: "examSiteId" },
  { title: "考试日期", dataIndex: "examDate", key: "examDate" },
  { title: "状态", dataIndex: "status", key: "status" }
];

const studentDisplayMap = computed(() =>
  Object.fromEntries(students.value.map((s) => [s.id, `${s.name} / ${s.phone}`]))
);

const studentOptions = computed(() =>
  students.value.map((s) => ({ label: `${s.name} / ${s.phone}`, value: s.id }))
);

const loadData = async () => {
  const [s, a, st] = await Promise.all([
    api.listExamSites({ page: 0, size: 50 }),
    api.listExamApplications({ page: 0, size: 50 }),
    api.listStudents({ page: 0, size: 500 })
  ]);
  if (s.data.success) sites.value = s.data.data.content;
  if (st.data.success) students.value = st.data.data.content || [];
  if (a.data.success) {
    apps.value = (a.data.data.content || []).map((x) => ({
      ...x,
      studentDisplay: studentDisplayMap.value[x.studentId] || `学员#${x.studentId}`
    }));
  }
};

const createSite = async () => {
  const payload = {
    ...siteForm,
    capacity: Number(siteForm.capacity),
    reservedCount: 0
  };
  const res = await api.createExamSite(payload);
  if (!res.data.success) return message.error(res.data.message || "创建失败");
  message.success("考场创建成功");
  loadData();
};

const createExam = async () => {
  if (!examForm.studentId) return message.warning("请选择学员（姓名 + 手机号）");
  const payload = {
    ...examForm,
    studentId: Number(examForm.studentId),
    examSiteId: Number(examForm.examSiteId)
  };
  const res = await api.createExamApplication(payload);
  if (!res.data.success) return message.error(res.data.message || "创建失败");
  message.success("报考申请提交成功");
  loadData();
};

onMounted(loadData);
</script>

<template>
  <a-space direction="vertical" style="display:flex;" :size="16">
    <a-card title="新增考场">
      <a-row :gutter="12">
        <a-col :xs="24" :sm="12" :lg="6"><a-input v-model:value="siteForm.name" placeholder="考场名称" /></a-col>
        <a-col :xs="24" :sm="12" :lg="6"><a-input v-model:value="siteForm.sessionName" placeholder="场次" /></a-col>
        <a-col :xs="24" :sm="12" :lg="6"><a-input-number v-model:value="siteForm.capacity" :min="1" style="width:100%;" /></a-col>
        <a-col :xs="24" :sm="12" :lg="6"><a-input v-model:value="siteForm.address" placeholder="地址" /></a-col>
      </a-row>
      <a-row :gutter="12" style="margin-top:12px;">
        <a-col :xs="24" :sm="12" :lg="6"><a-input v-model:value="siteForm.latitude" placeholder="纬度" /></a-col>
        <a-col :xs="24" :sm="12" :lg="6"><a-input v-model:value="siteForm.longitude" placeholder="经度" /></a-col>
        <a-col :xs="24" :sm="12" :lg="12"><a-input v-model:value="siteForm.routeGuide" placeholder="路线指引" /></a-col>
      </a-row>
      <a-button style="margin-top:12px;" type="primary" @click="createSite">保存考场</a-button>
    </a-card>
    <a-card title="报考申请">
      <a-row :gutter="12">
        <a-col :xs="24" :sm="12" :lg="6">
          <a-select
            v-model:value="examForm.studentId"
            :options="studentOptions"
            placeholder="学员姓名 + 手机号"
            show-search
            option-filter-prop="label"
          />
        </a-col>
        <a-col :xs="24" :sm="12" :lg="6"><a-input v-model:value="examForm.subjectCode" placeholder="科目代码" /></a-col>
        <a-col :xs="24" :sm="12" :lg="6"><a-input v-model:value="examForm.examSiteId" placeholder="考场ID" /></a-col>
        <a-col :xs="24" :sm="12" :lg="6"><a-date-picker style="width:100%;" @change="(_,s)=>examForm.examDate=s" /></a-col>
      </a-row>
      <a-button style="margin-top:12px;" type="primary" @click="createExam">提交报考</a-button>
    </a-card>
    <a-card title="考场列表">
      <a-table :columns="siteColumns" :data-source="sites" row-key="id" :pagination="{ pageSize: 6 }" />
    </a-card>
    <a-card title="报考列表">
      <a-table :columns="appColumns" :data-source="apps" row-key="id" :pagination="{ pageSize: 6 }" />
    </a-card>
  </a-space>
</template>
