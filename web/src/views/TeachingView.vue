<script setup>
import { computed, onMounted, reactive, ref } from "vue";
import { message } from "ant-design-vue";
import { api } from "../api";

const planForm = reactive({ studentPhone: "", coachName: undefined, subjectCode: "科目二", planDate: "", timeSlot: undefined });
const recordForm = reactive({ studentPhone: "", planId: undefined, coachName: undefined, subjectCode: "科目二", trainingDate: "", hours: 2, remark: "" });
const list = ref([]);
const progress = ref(null);
const students = ref([]);
const coachOptions = ref([]);
const pendingPlans = ref([]);
const currentUser = ref(null);

const subjectOptions = [
  { label: "科目二", value: "科目二" },
  { label: "科目三", value: "科目三" }
];

const timeSlotOptions = [
  { label: "09:00 - 11:00", value: "09:00 - 11:00" },
  { label: "14:00 - 16:00", value: "14:00 - 16:00" },
  { label: "16:30 - 18:30", value: "16:30 - 18:30" }
];

const columns = [
  { title: "学员", dataIndex: "studentDisplay", key: "studentDisplay" },
  { title: "教练", dataIndex: "coachName", key: "coachName" },
  { title: "科目", dataIndex: "subjectCode", key: "subjectCode" },
  { title: "训练日期", dataIndex: "trainingDate", key: "trainingDate" },
  { title: "学时", dataIndex: "hours", key: "hours" }
];

const studentDisplayMap = computed(() =>
  Object.fromEntries(students.value.map((s) => [s.id, `${s.name} / ${s.phone}`]))
);
const isCoachRole = computed(() => ["教练", "ROLE_COACH"].includes(currentUser.value?.role));
const currentDrivingSchoolId = computed(() => currentUser.value?.drivingSchoolId || null);

const loadCurrentUser = async () => {
  const res = await api.getMe();
  if (res.data.success) {
    currentUser.value = res.data.data;
  }
};

const loadCoaches = async () => {
  const params = { page: 0, size: 100, role: "教练" };
  if (isCoachRole.value) {
    if (!currentDrivingSchoolId.value) {
      coachOptions.value = [];
      return;
    }
    params.drivingSchoolId = currentDrivingSchoolId.value;
  }
  try {
    const res = await api.listUsers(params);
    if (res.data.success) {
      coachOptions.value = (res.data.data.content || []).map(c => ({
        label: c.displayName || c.username,
        value: c.displayName || c.username
      }));
    }
  } catch (e) {
    console.error("加载教练列表失败", e);
  }
};

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
  const params = { page: 0, size: 500 };
  if (isCoachRole.value) {
    if (!currentDrivingSchoolId.value) {
      students.value = [];
      return;
    }
    params.drivingSchoolId = currentDrivingSchoolId.value;
  }
  const res = await api.listStudents(params);
  if (res.data.success) {
    students.value = res.data.data.content || [];
  }
};

const createPlan = async () => {
  if (isCoachRole.value && !currentDrivingSchoolId.value) {
    return message.warning("当前教练未绑定驾校，无法创建课程计划");
  }
  if (!planForm.studentPhone) return message.warning("请输入学员手机号");
  if (!planForm.coachName || !planForm.timeSlot || !planForm.planDate) return message.warning("请完整填写预约信息");
  
  const student = students.value.find(s => s.phone === planForm.studentPhone);
  if (!student) return message.error(isCoachRole.value ? "未找到同驾校学员，请核对手机号" : "未找到该手机号对应的学员，请核对手机号");
  
  const payload = {
    studentId: student.id,
    coachName: `${planForm.coachName} (${planForm.timeSlot})`,
    subjectCode: planForm.subjectCode,
    planDate: planForm.planDate,
    status: "待上课"
  };

  const res = await api.createPlan(payload);
  if (!res.data.success) return message.error(res.data.message || "创建失败");
  message.success("课程计划(预约)创建成功");
  
  planForm.studentPhone = "";
  planForm.timeSlot = undefined;
};

const planOptions = computed(() => {
  return pendingPlans.value.map(p => ({
    label: `${p.planDate} | ${p.subjectCode} | ${p.coachName}`,
    value: p.id,
    rawData: p
  }));
});

const onStudentPhoneChangeForRecord = async () => {
  recordForm.planId = undefined;
  recordForm.coachName = undefined;
  recordForm.subjectCode = "科目二";
  recordForm.trainingDate = "";
  pendingPlans.value = [];

  if (!recordForm.studentPhone || recordForm.studentPhone.length < 11) return;
  
  const student = students.value.find(s => s.phone === recordForm.studentPhone);
  if (!student) return;

  try {
    const res = await api.listPlans({ page: 0, size: 50, studentId: student.id });
    if (res.data.success) {
      pendingPlans.value = (res.data.data.content || []).filter(p => p.status === '待上课');
    }
  } catch (e) {
    console.error("加载学员计划失败", e);
  }
};

const onPlanSelect = (val, option) => {
  const p = option.rawData;
  if (p) {
    // 提取教练姓名 (去掉时间段后缀)
    const match = p.coachName.match(/^(.*?)\s*\(/);
    recordForm.coachName = match ? match[1] : p.coachName;
    recordForm.subjectCode = p.subjectCode;
    recordForm.trainingDate = p.planDate;
  }
};

const createRecord = async () => {
  if (!recordForm.studentPhone) return message.warning("请输入学员手机号");
  if (!recordForm.planId) return message.warning("请选择要标记完成的课程计划");
  if (!recordForm.coachName || !recordForm.trainingDate) return message.warning("请完整填写训练信息");
  
  const student = students.value.find(s => s.phone === recordForm.studentPhone);
  if (!student) return message.error("未找到该手机号对应的学员，请核对手机号");
  
  const payload = { 
    studentId: student.id, 
    coachName: recordForm.coachName, 
    subjectCode: recordForm.subjectCode, 
    trainingDate: recordForm.trainingDate, 
    hours: Number(recordForm.hours),
    remark: recordForm.remark
  };
  
  const res = await api.createRecord(payload);
  if (!res.data.success) return message.error(res.data.message || "保存失败");
  
  // 同步将对应的计划状态改为“已完成”
  try {
    await api.updatePlanStatus(recordForm.planId, "已完成");
  } catch (e) {
    console.error("更新计划状态失败", e);
  }

  message.success("训练记录保存成功，计划已标记完成");
  
  recordForm.studentPhone = "";
  recordForm.planId = undefined;
  recordForm.remark = "";
  pendingPlans.value = [];
  loadRecords();
};

const queryProgress = async () => {
  if (!recordForm.studentPhone) return message.warning("请输入学员手机号");
  const student = students.value.find(s => s.phone === recordForm.studentPhone);
  if (!student) return message.error("未找到该手机号对应的学员，请核对手机号");
  
  const res = await api.getProgress(student.id);
  if (res.data.success) progress.value = res.data.data;
};

onMounted(async () => {
  await loadCurrentUser();
  await loadCoaches();
  await loadStudents();
  await loadRecords();
});
</script>

<template>
  <a-space direction="vertical" style="display:flex;" :size="16">
    <a-card title="新增课程计划(预约)">
      <a-alert
        v-if="isCoachRole && !currentDrivingSchoolId"
        type="warning"
        show-icon
        message="当前教练账号尚未绑定驾校，暂时无法创建计划。"
        style="margin-bottom: 12px;"
      />
      <a-row :gutter="12">
        <a-col :xs="24" :sm="12" :lg="5">
          <a-input v-model:value="planForm.studentPhone" :placeholder="isCoachRole ? '输入同驾校学员手机号' : '输入学员手机号'" />
        </a-col>
        <a-col :xs="24" :sm="12" :lg="4">
          <a-select
            v-model:value="planForm.coachName"
            :options="coachOptions"
            :placeholder="isCoachRole ? '选择同驾校教练' : '选择教练'"
            :disabled="isCoachRole && !currentDrivingSchoolId"
            style="width:100%;"
          />
        </a-col>
        <a-col :xs="24" :sm="12" :lg="3">
          <a-select v-model:value="planForm.subjectCode" :options="subjectOptions" placeholder="选择科目" style="width:100%;" />
        </a-col>
        <a-col :xs="24" :sm="12" :lg="4">
          <a-date-picker style="width:100%;" value-format="YYYY-MM-DD" v-model:value="planForm.planDate" placeholder="预约日期" />
        </a-col>
        <a-col :xs="24" :sm="12" :lg="4">
          <a-select v-model:value="planForm.timeSlot" :options="timeSlotOptions" placeholder="时间段" style="width:100%;" />
        </a-col>
        <a-col :xs="24" :sm="12" :lg="4">
          <a-button type="primary" block :disabled="isCoachRole && !currentDrivingSchoolId" @click="createPlan">创建预约</a-button>
        </a-col>
      </a-row>
    </a-card>
    <a-card title="新增训练记录 (核销计划)">
      <a-row :gutter="12">
        <a-col :xs="24" :sm="12" :lg="5">
          <a-input v-model:value="recordForm.studentPhone" placeholder="输入学员手机号" @blur="onStudentPhoneChangeForRecord" @pressEnter="onStudentPhoneChangeForRecord" />
        </a-col>
        <a-col :xs="24" :sm="12" :lg="9">
          <a-select 
            v-model:value="recordForm.planId" 
            :options="planOptions" 
            placeholder="选择该学员的待上课计划" 
            style="width:100%;" 
            @change="onPlanSelect"
            :not-found-content="recordForm.studentPhone ? '未找到待上课计划' : '请先输入学员手机号并回车'"
          />
        </a-col>
        <a-col :xs="24" :sm="12" :lg="4">
          <a-input-number :min="0.5" :step="0.5" style="width:100%;" placeholder="本次学时" />
        </a-col>
        <a-col :xs="24" :sm="12" :lg="6">
          <a-button type="primary" block @click="createRecord">完成训练并记录学时</a-button>
        </a-col>
      </a-row>
      <a-input v-model:value="recordForm.remark" style="margin-top:12px;" placeholder="备注信息 (可选)" />
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
