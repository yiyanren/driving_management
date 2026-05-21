<script setup>
import { onMounted, ref, computed } from "vue";
import { message } from "ant-design-vue";
import { api } from "../api";
import { useAuthStore } from "../stores/auth";

const auth = useAuthStore();
const progress = ref(null);
const studentProfile = ref(null);
const loading = ref(false);

const loadProgress = async () => {
  loading.value = true;
  try {
    const profileRes = await api.getStudentMe();
    if (profileRes.data.success) {
      studentProfile.value = profileRes.data.data;
    }
    if (studentProfile.value?.id) {
      const res = await api.getProgress(studentProfile.value.id);
      if (res.data.success) {
        progress.value = res.data.data;
      }
    }
  } catch (e) {
    message.error("加载进度失败");
  } finally {
    loading.value = false;
  }
};

const currentStep = computed(() => {
  if (!studentProfile.value) return 0;
  if (studentProfile.value.subjectFourPassed) return 4;
  if (studentProfile.value.subjectThreePassed) return 3;
  if (studentProfile.value.subjectTwoPassed) return 2;
  if (studentProfile.value.subjectOnePassed) return 1;
  return 0;
});

const currentSubjectStatus = computed(() => {
  if (!studentProfile.value) return "待开始";
  if (studentProfile.value.subjectFourPassed) return "科目四已完成";
  if (studentProfile.value.subjectThreePassed) return "当前进行到科目四";
  if (studentProfile.value.subjectTwoPassed) return "当前进行到科目三";
  if (studentProfile.value.subjectOnePassed) return "当前进行到科目二";
  return "当前进行到科目一";
});

const currentSubjectTagColor = computed(() => {
  if (!studentProfile.value) return "default";
  if (studentProfile.value.subjectFourPassed) return "success";
  if (studentProfile.value.subjectThreePassed) return "processing";
  if (studentProfile.value.subjectOnePassed || studentProfile.value.subjectTwoPassed) return "blue";
  return "orange";
});

onMounted(() => {
  loadProgress();
});
</script>

<template>
  <a-spin :spinning="loading">
    <div class="welcome-banner" style="background: #fff; padding: 24px; border-radius: 8px; margin-bottom: 24px;">
      <h2>欢迎回来，{{ auth.user?.displayName || auth.user?.username }}！</h2>
      <p style="color: #8c8c8c;">今天也要继续努力哦，离拿证又近了一步！</p>
    </div>

    <a-card title="我的学车进度" style="margin-bottom: 24px;">
      <a-space style="margin-bottom: 16px;">
        <span>当前状态：</span>
        <a-tag :color="currentSubjectTagColor">{{ currentSubjectStatus }}</a-tag>
      </a-space>
      <a-steps :current="currentStep">
        <a-step title="科目一" description="理论基础" />
        <a-step title="科目二" description="场地驾驶" />
        <a-step title="科目三" description="道路驾驶" />
        <a-step title="科目四" description="安全文明" />
      </a-steps>
      
      <a-divider />
      
      <a-row :gutter="16">
        <a-col :span="12">
          <a-statistic 
            title="科目二（场地驾驶）" 
            :value="progress?.subjectHours?.K2 || 0" 
            suffix="/ 14 小时" 
            :value-style="{ color: (progress?.subjectHours?.K2 || 0) >= 14 ? '#3f8600' : '#cf1322' }"
          >
            <template #prefix>实学</template>
          </a-statistic>
          <a-progress :percent="Math.min(100, Math.round(((progress?.subjectHours?.K2 || 0) / 14) * 100))" />
        </a-col>
        <a-col :span="12">
          <a-statistic 
            title="科目三（道路驾驶）" 
            :value="progress?.subjectHours?.K3 || 0" 
            suffix="/ 20 小时" 
            :value-style="{ color: (progress?.subjectHours?.K3 || 0) >= 20 ? '#3f8600' : '#cf1322' }"
          >
            <template #prefix>实学</template>
          </a-statistic>
          <a-progress :percent="Math.min(100, Math.round(((progress?.subjectHours?.K3 || 0) / 20) * 100))" />
        </a-col>
      </a-row>
    </a-card>
  </a-spin>
</template>

<style scoped>
</style>
