<script setup>
import { computed, onMounted, ref } from "vue";
import { message } from "ant-design-vue";
import { api } from "../api";

// 提供跳转到交管12123的具体业务入口
const applyUrl = "https://gab.122.gov.cn/m/login#/pcView/exam/apply";
const queryUrl = "https://hb.122.gov.cn/web/html/?type=1&ticket=NIL&code=E_A_1023#/nView/exam/limitpub";

const loading = ref(false);
const studentProfile = ref(null);
const progress = ref(null);

const subjectRequirementMap = {
  K2: { label: "科目二", requiredHours: 14 },
  K3: { label: "科目三", requiredHours: 20 }
};

const openUrl = (url) => {
  window.open(url, "_blank");
};

const loadStudentContext = async () => {
  loading.value = true;
  try {
    const profileRes = await api.getStudentMe();
    if (!profileRes.data.success) {
      throw new Error("学员信息加载失败");
    }
    studentProfile.value = profileRes.data.data;

    if (studentProfile.value?.id) {
      const progressRes = await api.getProgress(studentProfile.value.id);
      if (progressRes.data.success) {
        progress.value = progressRes.data.data;
      }
    }
  } catch (error) {
    message.error("加载考试申请校验信息失败");
  } finally {
    loading.value = false;
  }
};

const currentExamSubject = computed(() => {
  if (!studentProfile.value) return null;
  if (!studentProfile.value.subjectOnePassed) return { code: "K1", label: "科目一" };
  if (!studentProfile.value.subjectTwoPassed) return { code: "K2", label: "科目二" };
  if (!studentProfile.value.subjectThreePassed) return { code: "K3", label: "科目三" };
  if (!studentProfile.value.subjectFourPassed) return { code: "K4", label: "科目四" };
  return { code: "DONE", label: "全部科目已完成" };
});

const getSubjectHours = (subjectCode) => {
  const rawHours = progress.value?.subjectHours?.[subjectCode];
  return Number(rawHours || 0);
};

const handleApplyClick = async () => {
  if (!studentProfile.value) {
    await loadStudentContext();
  }
  if (!studentProfile.value) {
    return;
  }

  const currentSubject = currentExamSubject.value;
  if (!currentSubject) {
    message.warning("暂无法识别当前考试进度，请稍后重试");
    return;
  }

  const requirement = subjectRequirementMap[currentSubject.code];
  if (requirement) {
    const currentHours = getSubjectHours(currentSubject.code);
    if (currentHours < requirement.requiredHours) {
      message.warning(
        `当前进行到${requirement.label}，实学学时仅 ${currentHours} / ${requirement.requiredHours} 小时，暂不满足申请考试条件`
      );
      return;
    }
  }

  openUrl(applyUrl);
};

onMounted(() => {
  loadStudentContext();
});
</script>

<template>
  <div class="exam-guide-container">
    <a-card class="guide-card" :bordered="false">
      <div class="guide-header">
       
        <h2>交通安全综合服务管理平台</h2>
        <p class="subtitle">全国统一的机动车驾驶人考试预约官方平台</p>
      </div>

      <div class="guide-content">
        <a-row :gutter="[24, 24]">
          <a-col :xs="24" :md="12">
            <div class="action-box">
              <div class="icon-wrap bg-blue">
                <span class="iconfont">📝</span>
              </div>
              <h3>申请考试</h3>
              <p>在线自主选择考试场地和考试场次，提交各科目考试预约申请。</p>
              <a-button type="primary" size="large" :loading="loading" @click="handleApplyClick">前往申请</a-button>
            </div>
          </a-col>
          <a-col :xs="24" :md="12">
            <div class="action-box">
              <div class="icon-wrap bg-green">
                <span class="iconfont">🔍</span>
              </div>
              <h3>预约查询</h3>
              <p>实时查询您的考试预约结果、排队排名以及历史考试成绩。</p>
              <a-button size="large" @click="openUrl(queryUrl)">点击查询</a-button>
            </div>
          </a-col>
        </a-row>
      </div>

      <div class="tips-section">
        <h4>温馨提示：</h4>
        <ul>
          <li>所有的驾照考试预约必须通过公安部官方网站或“交管12123”APP进行。</li>
          <li>驾校仅提供培训服务，不具备直接安排考试的权限，请警惕任何承诺“包过”、“代约”的信息。</li>
          <li>预约前请确保您的累计培训学时已经满足车管所的最低要求，否则将无法通过预约审核。</li>
        </ul>
      </div>
    </a-card>
  </div>
</template>

<style scoped>
.exam-guide-container {
  padding: 24px;
  min-height: calc(100vh - 64px);
  background: #f0f2f5;
  display: flex;
  justify-content: center;
  align-items: flex-start;
}

.guide-card {
  width: 100%;
  max-width: 900px;
  border-radius: 12px;
  box-shadow: 0 4px 12px rgba(0,0,0,0.05);
}

.guide-header {
  text-align: center;
  padding: 40px 0 20px;
  border-bottom: 1px solid #f0f0f0;
}

.logo {
  height: 60px;
  margin-bottom: 16px;
}

.guide-header h2 {
  font-size: 28px;
  color: #1890ff;
  margin-bottom: 8px;
}

.subtitle {
  color: #8c8c8c;
  font-size: 16px;
}

.guide-content {
  padding: 40px 20px;
}

.action-box {
  text-align: center;
  padding: 30px 20px;
  border: 1px solid #e8e8e8;
  border-radius: 8px;
  transition: all 0.3s;
  height: 100%;
  display: flex;
  flex-direction: column;
  align-items: center;
}

.action-box:hover {
  box-shadow: 0 8px 16px rgba(0,0,0,0.08);
  transform: translateY(-5px);
  border-color: #1890ff;
}

.icon-wrap {
  width: 72px;
  height: 72px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-bottom: 20px;
}

.bg-blue { background: #e6f7ff; color: #1890ff; }
.bg-green { background: #f6ffed; color: #52c41a; }

.iconfont {
  font-size: 32px;
}

.action-box h3 {
  font-size: 20px;
  margin-bottom: 12px;
}

.action-box p {
  color: #666;
  margin-bottom: 24px;
  flex-grow: 1;
}

.tips-section {
  background: #fffbe6;
  border: 1px solid #ffe58f;
  padding: 20px 24px;
  border-radius: 8px;
  margin-top: 20px;
}

.tips-section h4 {
  color: #faad14;
  margin-bottom: 12px;
  font-size: 16px;
}

.tips-section ul {
  padding-left: 20px;
  color: #555;
  margin: 0;
}

.tips-section li {
  margin-bottom: 8px;
  line-height: 1.5;
}
.tips-section li:last-child {
  margin-bottom: 0;
}
</style>
