<script setup>
import { onMounted, ref, computed } from "vue";
import { useAuthStore } from "../stores/auth";
import { message } from "ant-design-vue";
import { api } from "../api";
import dayjs from "dayjs";

const auth = useAuthStore();
const coachName = computed(() => auth.user?.displayName || auth.user?.username);

const loading = ref(false);
const upcomingPlans = ref([]);
const students = ref([]);

const studentDisplayMap = computed(() =>
  Object.fromEntries(students.value.map((s) => [s.id, `${s.name} / ${s.phone}`]))
);

const columns = [
  { title: "预约日期", dataIndex: "planDate", key: "planDate" },
  { title: "时间段", dataIndex: "timeSlot", key: "timeSlot" },
  { title: "学员", dataIndex: "studentDisplay", key: "studentDisplay" },
  { title: "科目", dataIndex: "subjectCode", key: "subjectCode" },
  { title: "状态", dataIndex: "status", key: "status" }
];

const loadData = async () => {
  loading.value = true;
  try {
    const [stRes, planRes] = await Promise.all([
      api.listStudents({ page: 0, size: 500 }),
      api.listPlans({ page: 0, size: 50, coachName: coachName.value })
    ]);
    
    if (stRes.data.success) {
      students.value = stRes.data.data.content || [];
    }
    
    if (planRes.data.success) {
      const today = dayjs().format("YYYY-MM-DD");
      // 过滤出从今天开始的未来计划，并解析出具体的时间段
      let plans = (planRes.data.data.content || [])
        .filter(p => p.planDate >= today)
        .map(p => {
          let timeSlot = "-";
          // 从 coachName 字段中提取时间段 (例如："张教练 (09:00 - 11:00)")
          const match = p.coachName.match(/\((.*?)\)/);
          if (match && match[1]) {
            timeSlot = match[1];
          }
          return {
            ...p,
            timeSlot,
            studentDisplay: studentDisplayMap.value[p.studentId] || `学员#${p.studentId}`
          };
        });
      
      // 按日期和时间段排序
      plans.sort((a, b) => {
        if (a.planDate !== b.planDate) return a.planDate.localeCompare(b.planDate);
        return a.timeSlot.localeCompare(b.timeSlot);
      });
      
      upcomingPlans.value = plans;
    }
  } catch (e) {
    message.error("加载数据失败");
  } finally {
    loading.value = false;
  }
};

onMounted(() => {
  loadData();
});
</script>

<template>
  <a-space direction="vertical" style="display:flex;" :size="16">
    <div class="welcome-banner">
      <div class="welcome-text">
        <h2>欢迎回来，{{ coachName }}！</h2>
        <p>今天是 {{ dayjs().format("YYYY年MM月DD日") }}，祝您教学顺利！</p>
      </div>
    </div>

    <a-card title="近期的学员预约 (从今日起)">
      <a-table 
        :loading="loading" 
        :columns="columns" 
        :data-source="upcomingPlans" 
        row-key="id" 
        :pagination="{ pageSize: 10 }"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'status'">
            <a-tag color="processing">{{ record.status }}</a-tag>
          </template>
        </template>
      </a-table>
    </a-card>
  </a-space>
</template>

<style scoped>
.welcome-banner {
  background: linear-gradient(135deg, #1890ff 0%, #36cfc9 100%);
  border-radius: 8px;
  padding: 32px 24px;
  color: #fff;
  display: flex;
  align-items: center;
}
.welcome-text h2 {
  color: #fff;
  margin-top: 0;
  margin-bottom: 8px;
  font-weight: 500;
}
.welcome-text p {
  margin: 0;
  font-size: 16px;
  opacity: 0.9;
}
</style>