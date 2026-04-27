<script setup>
import { computed, onMounted, reactive, ref } from "vue";
import { message } from "ant-design-vue";
import { api } from "../api";

const planForm = reactive({ studentId: undefined, coachName: "", subjectCode: "K2", planDate: "", status: "待上课" });
const recordForm = reactive({ studentId: undefined, coachName: "", subjectCode: "K2", trainingDate: "", hours: 2, remark: "" });
const list = ref([]);
const progress = ref(null);
const students = ref([]);

const columns = [
  { title: "ID", dataIndex: "id", key: "id", width: 80 },
  { title: "学员", dataIndex: "studentDisplay", key: "studentDisplay" },
  { title: "教练", dataIndex: "coachName", key: "coachName" },
  { title: "科目", dataIndex: "subjectCode", key: "subjectCode" },
  { title: "训练日期", dataIndex: "trainingDate", key: "trainingDate" },
  { title: "学时", dataIndex: "hours", key: "hours" }
];

const studentDisplayMap = computed(() =>
  Object.fromEntries(students.value.map((s) => [s.id, `${s.name} / ${s.phone}`]))
);

const studentOptions = computed(() =>
  students.value.map((s) => ({
    label: `${s.name} / ${s.phone}`,
    value: s.id
  }))
);

const loadRecords = async () => {
  const res = await api.listRecords({ page: 0, size: 50 });
  if (res.data.success) {
    list.value = (res.data.data.content || []).map((x) => ({
      ...x,
      studentDisplay: studentDisplayMap.value[x.studentId] || `学员#${x.studentId}`
    }));
  }
};

const loadStudents = async () => {
  const res = await api.listStudents({ page: 0, size: 500 });
  if (res.data.success) {
    students.value = res.data.data.content || [];
  }
};

const createPlan = async () => {
  if (!planForm.studentId) return message.warning("请先选择学员（姓名 + 手机号）");
  const res = await api.createPlan({ ...planForm, studentId: Number(planForm.studentId) });
  if (!res.data.success) return message.error(res.data.message || "创建失败");
  message.success("课程计划创建成功");
};

const createRecord = async () => {
  if (!recordForm.studentId) return message.warning("请先选择学员（姓名 + 手机号）");
  const payload = { ...recordForm, studentId: Number(recordForm.studentId), hours: Number(recordForm.hours) };
  const res = await api.createRecord(payload);
  if (!res.data.success) return message.error(res.data.message || "保存失败");
  message.success("训练记录保存成功");
  loadRecords();
};

const queryProgress = async () => {
  if (!recordForm.studentId) return message.warning("请先选择学员（姓名 + 手机号）");
  const res = await api.getProgress(Number(recordForm.studentId));
  if (res.data.success) progress.value = res.data.data;
};

onMounted(async () => {
  await loadStudents();
  await loadRecords();
});
</script>

<template>
  <a-space direction="vertical" style="display:flex;" :size="16">
    <a-card title="课程计划">
      <a-row :gutter="12">
        <a-col :xs="24" :sm="12" :lg="6">
          <a-select
            v-model:value="planForm.studentId"
            :options="studentOptions"
            placeholder="学员姓名 + 手机号"
            show-search
            option-filter-prop="label"
          />
        </a-col>
        <a-col :xs="24" :sm="12" :lg="4"><a-input v-model:value="planForm.coachName" placeholder="教练名" /></a-col>
        <a-col :xs="24" :sm="12" :lg="4"><a-input v-model:value="planForm.subjectCode" placeholder="科目代码" /></a-col>
        <a-col :xs="24" :sm="12" :lg="4"><a-date-picker style="width:100%;" @change="(_,s)=>planForm.planDate=s" /></a-col>
        <a-col :xs="24" :sm="12" :lg="4"><a-input v-model:value="planForm.status" placeholder="状态" /></a-col>
        <a-col :xs="24" :sm="12" :lg="4"><a-button type="primary" block @click="createPlan">创建计划</a-button></a-col>
      </a-row>
    </a-card>
    <a-card title="训练记录">
      <a-row :gutter="12">
        <a-col :xs="24" :sm="12" :lg="6">
          <a-select
            v-model:value="recordForm.studentId"
            :options="studentOptions"
            placeholder="学员姓名 + 手机号"
            show-search
            option-filter-prop="label"
          />
        </a-col>
        <a-col :xs="24" :sm="12" :lg="4"><a-input v-model:value="recordForm.coachName" placeholder="教练名" /></a-col>
        <a-col :xs="24" :sm="12" :lg="4"><a-input v-model:value="recordForm.subjectCode" placeholder="科目代码" /></a-col>
        <a-col :xs="24" :sm="12" :lg="4"><a-date-picker style="width:100%;" @change="(_,s)=>recordForm.trainingDate=s" /></a-col>
        <a-col :xs="24" :sm="12" :lg="4"><a-input-number v-model:value="recordForm.hours" :min="0.5" :step="0.5" style="width:100%;" /></a-col>
        <a-col :xs="24" :sm="12" :lg="4"><a-button type="primary" block @click="createRecord">保存记录</a-button></a-col>
      </a-row>
      <a-input v-model:value="recordForm.remark" style="margin-top:12px;" placeholder="备注" />
      <a-space style="margin-top:12px;">
        <a-button @click="queryProgress">查询学时进度</a-button>
        <span v-if="progress">总学时: {{ progress.totalHours }}，可报考: {{ progress.readyForExam ? "是" : "否" }}</span>
      </a-space>
    </a-card>
    <a-card title="训练记录列表">
      <a-table :columns="columns" :data-source="list" row-key="id" :pagination="{ pageSize: 8 }" />
    </a-card>
  </a-space>
</template>
