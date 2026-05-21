<script setup>
import { onMounted, reactive, ref, computed } from "vue";
import { message } from "ant-design-vue";
import { api } from "../api";

const loading = ref(false);
const form = reactive({
  subjectCode: "K2",
  coachName: "",
  planDate: "",
  timeSlot: ""
});

const myReservations = ref([]);
const studentId = ref(null);
const studentProfile = ref(null);

const subjectOptions = [
  { label: "科目二", value: "K2" },
  { label: "科目三", value: "K3" }
];

const coachOptions = ref([]);
const hasDrivingSchool = computed(() => Boolean(studentProfile.value?.drivingSchoolId));

const timeSlotOptions = [
  { label: "09:00 - 11:00", value: "09:00 - 11:00" },
  { label: "14:00 - 16:00", value: "14:00 - 16:00" },
  { label: "16:30 - 18:30", value: "16:30 - 18:30" }
];

const loadCoaches = async () => {
  const drivingSchoolId = studentProfile.value?.drivingSchoolId;
  if (!drivingSchoolId) {
    coachOptions.value = [];
    return;
  }
  try {
    const res = await api.listUsers({ page: 0, size: 100, role: "教练", drivingSchoolId });
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

const loadStudentContext = async () => {
  const res = await api.getStudentMe();
  if (res.data.success) {
    studentProfile.value = res.data.data;
    studentId.value = res.data.data?.id || null;
  }
};

const loadMyReservations = async () => {
  loading.value = true;
  try {
    if (!studentId.value) {
      await loadStudentContext();
    }
    if (!studentId.value) {
      myReservations.value = [];
      return;
    }
    const res = await api.listPlans({ page: 0, size: 50, studentId: studentId.value });
    if (res.data.success) {
      myReservations.value = res.data.data.content || [];
    }
  } catch (e) {
    message.error("加载记录失败");
  } finally {
    loading.value = false;
  }
};

const submitReservation = async () => {
  if (!hasDrivingSchool.value) {
    return message.warning("请先在报名管理中选择驾校，再预约培训");
  }
  if (!form.subjectCode || !form.coachName || !form.planDate || !form.timeSlot) {
    return message.warning("请完整填写预约信息");
  }

  loading.value = true;
  try {
    // 检查容量（简单模拟前端校验，查询所有人这天的这个教练这个时间段的预约）
    const allRes = await api.listPlans({ page: 0, size: 100 });
    if (allRes.data.success) {
      const plans = allRes.data.data.content || [];
      const coachTimeSlot = `${form.coachName} (${form.timeSlot})`;
      const count = plans.filter(p => p.planDate === form.planDate && p.coachName === coachTimeSlot).length;
      
      if (count >= 3) {
        message.error("该教练此时间段预约人数已满（上限3人），请选择其他时间段或其他教练");
        loading.value = false;
        return;
      }
    }

    const payload = {
      studentId: studentId.value,
      subjectCode: form.subjectCode,
      coachName: `${form.coachName} (${form.timeSlot})`,
      planDate: form.planDate,
      status: "待上课"
    };

    const res = await api.createPlan(payload);
    if (res.data.success) {
      message.success("预约成功！");
      form.coachName = "";
      form.timeSlot = "";
      loadMyReservations();
    } else {
      message.error(res.data.message || "预约失败");
    }
  } catch (e) {
    message.error("预约出错");
  } finally {
    loading.value = false;
  }
};

const columns = [
  { title: "预约日期", dataIndex: "planDate", key: "planDate" },
  { title: "教练及时间段", dataIndex: "coachName", key: "coachName" },
  { title: "科目", dataIndex: "subjectCode", key: "subjectCode" },
  { title: "状态", dataIndex: "status", key: "status" }
];

onMounted(() => {
  loadStudentContext()
    .then(() => Promise.all([loadCoaches(), loadMyReservations()]))
    .catch(() => {
      message.error("加载学员信息失败");
    });
});
</script>

<template>
  <a-space direction="vertical" style="display:flex;" :size="16">
    <a-card title="新增预约">
      <a-spin :spinning="loading">
        <a-form layout="vertical">
          <a-row :gutter="16">
            <a-col :span="6">
              <a-form-item label="科目">
                <a-select v-model:value="form.subjectCode" :options="subjectOptions" placeholder="选择科目" />
              </a-form-item>
            </a-col>
            <a-col :span="6">
              <a-form-item label="教练">
                <a-select
                  v-model:value="form.coachName"
                  :options="coachOptions"
                  :disabled="!hasDrivingSchool"
                  :placeholder="hasDrivingSchool ? '选择同驾校教练' : '请先完成驾校报名'"
                />
              </a-form-item>
            </a-col>
            <a-col :span="6">
              <a-form-item label="日期">
                <a-date-picker style="width:100%;" value-format="YYYY-MM-DD" v-model:value="form.planDate" placeholder="选择日期" />
              </a-form-item>
            </a-col>
            <a-col :span="6">
              <a-form-item label="时间段">
                <a-select v-model:value="form.timeSlot" :options="timeSlotOptions" placeholder="选择时间段" />
              </a-form-item>
            </a-col>
          </a-row>
          <a-alert
            v-if="!hasDrivingSchool"
            type="info"
            show-icon
            message="请先在报名管理中选择驾校，预约培训时只能选择同驾校教练。"
            style="margin-bottom: 12px;"
          />
          <a-button type="primary" :disabled="!hasDrivingSchool" @click="submitReservation">提交预约</a-button>
        </a-form>
      </a-spin>
    </a-card>

    <a-card title="我的预约记录">
      <a-table :columns="columns" :data-source="myReservations" row-key="id" :pagination="{ pageSize: 5 }" />
    </a-card>
  </a-space>
</template>
