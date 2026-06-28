<script setup>
import { computed, onMounted, reactive, ref } from "vue";
import { message } from "ant-design-vue";
import { api } from "../api";
import { useAuthStore } from "../stores/auth";

const auth = useAuthStore();
const isStudent = computed(() => auth.role === "学员" || auth.role === "ROLE_STUDENT");
const isCoach = computed(() => auth.role === "教练");
const LICENSE_TYPE_OPTIONS = ["A1", "A2", "A3", "B1", "B2", "C1", "C2", "C5", "D", "E", "F", "M", "N", "P"];
const studentHasEnrollment = computed(() => (
  studentProfile.value?.status === "已报名"
  && !!studentProfile.value?.drivingSchoolId
  && !!studentProfile.value?.licenseType
));

const schools = ref([]);
const currentUser = ref(null);

const profileLoading = ref(false);
const submitLoading = ref(false);
const studentProfile = ref(null);
const selectedSchoolId = ref(undefined);
const selectedLicenseType = ref(undefined);

const recordsLoading = ref(false);
const records = ref([]);
const total = ref(0);
const query = reactive({
  page: 0,
  size: 10,
  keyword: "",
  drivingSchoolId: undefined
});

const columns = [
  { title: "姓名", dataIndex: "name", key: "name" },
  { title: "手机号", dataIndex: "phone", key: "phone" },
  { title: "身份证", dataIndex: "idCard", key: "idCard" },
  { title: "所属驾校", dataIndex: "schoolName", key: "schoolName" },
  { title: "报名类型", dataIndex: "licenseType", key: "licenseType" },
  { title: "报名状态", dataIndex: "status", key: "status" }
];

const schoolOptions = computed(() => schools.value.map((item) => ({ label: item.name, value: item.id })));
const licenseTypeOptions = LICENSE_TYPE_OPTIONS.map((item) => ({ label: item, value: item }));

const getSchoolName = (drivingSchoolId) => {
  const school = schools.value.find((item) => item.id === drivingSchoolId);
  return school ? school.name : "未选择";
};

const loadSchools = async () => {
  try {
    const res = await api.listAllDrivingSchools();
    if (res.data.success) {
      schools.value = res.data.data || [];
    }
  } catch (e) {
    message.error("获取驾校列表失败");
  }
};

const loadCurrentUser = async () => {
  try {
    const res = await api.getMe();
    if (res.data.success) {
      currentUser.value = res.data.data;
    }
  } catch (e) {
    message.error("获取当前用户信息失败");
  }
};

const loadStudentProfile = async () => {
  profileLoading.value = true;
  try {
    const res = await api.getStudentMe();
    if (res.data.success) {
      studentProfile.value = res.data.data;
      selectedSchoolId.value = res.data.data.drivingSchoolId;
      selectedLicenseType.value = res.data.data.licenseType;
    }
  } catch (e) {
    message.error(e?.response?.data?.message || "获取报名信息失败");
  } finally {
    profileLoading.value = false;
  }
};

const submitEnrollment = async () => {
  if (!selectedSchoolId.value) {
    return message.warning("请选择报名驾校");
  }
  if (!selectedLicenseType.value) {
    return message.warning("请选择报名类型");
  }
  submitLoading.value = true;
  try {
    const res = await api.submitStudentEnrollment(selectedSchoolId.value, selectedLicenseType.value);
    if (!res.data.success) {
      return message.error(res.data.message || "报名失败");
    }
    message.success("报名成功");
    await loadStudentProfile();
  } catch (e) {
    message.error(e?.response?.data?.message || "报名失败");
  } finally {
    submitLoading.value = false;
  }
};

const loadEnrollmentRecords = async () => {
  recordsLoading.value = true;
  try {
    const params = {
      page: query.page,
      size: query.size,
      keyword: query.keyword
    };
    if (!isCoach.value && query.drivingSchoolId) {
      params.drivingSchoolId = query.drivingSchoolId;
    }
    const res = await api.listEnrollmentRecords(params);
    if (res.data.success) {
      const mapped = (res.data.data.content || []).map((item) => ({
        ...item,
        schoolName: getSchoolName(item.drivingSchoolId)
      }));
      mapped.sort((a, b) => {
        const schoolA = a.drivingSchoolId || 0;
        const schoolB = b.drivingSchoolId || 0;
        if (schoolA !== schoolB) {
          return schoolA - schoolB;
        }
        return b.id - a.id;
      });
      records.value = mapped;
      total.value = res.data.data.totalElements;
    }
  } catch (e) {
    message.error(e?.response?.data?.message || "获取报名记录失败");
  } finally {
    recordsLoading.value = false;
  }
};

const handleSearch = () => {
  query.page = 0;
  loadEnrollmentRecords();
};

onMounted(async () => {
  await loadSchools();
  if (isStudent.value) {
    await loadStudentProfile();
    return;
  }
  await loadCurrentUser();
  await loadEnrollmentRecords();
});
</script>

<template>
  <a-space direction="vertical" style="display: flex;" :size="16">
    <template v-if="isStudent">
      <a-card title="报名管理" :loading="profileLoading">
        <template v-if="studentProfile">
          <a-alert
            :type="studentHasEnrollment ? 'success' : 'info'"
            show-icon
            :message="studentHasEnrollment ? '当前账号已完成报名' : '当前账号尚未报名或报名信息不完整，请先补全驾校和报名类型'"
            style="margin-bottom: 16px;"
          />
          <a-descriptions bordered :column="1">
            <a-descriptions-item label="姓名">{{ studentProfile.name }}</a-descriptions-item>
            <a-descriptions-item label="手机号">{{ studentProfile.phone }}</a-descriptions-item>
            <a-descriptions-item label="报名状态">
              <a-tag :color="studentHasEnrollment ? 'green' : 'orange'">{{ studentHasEnrollment ? "已报名" : "未报名" }}</a-tag>
            </a-descriptions-item>
            <a-descriptions-item label="所属驾校">{{ getSchoolName(studentProfile.drivingSchoolId) }}</a-descriptions-item>
            <a-descriptions-item label="报名类型">{{ studentProfile.licenseType || "未选择" }}</a-descriptions-item>
          </a-descriptions>

          <a-card v-if="!studentHasEnrollment" size="small" style="margin-top: 16px;">
            <a-space direction="vertical" style="display: flex;">
              <a-select
                v-model:value="selectedSchoolId"
                placeholder="请选择要报名的驾校"
                show-search
                option-filter-prop="label"
                :options="schoolOptions"
              />
              <a-select
                v-model:value="selectedLicenseType"
                placeholder="请选择报名类型"
                :options="licenseTypeOptions"
              />
              <a-button type="primary" :loading="submitLoading" @click="submitEnrollment">确认报名</a-button>
            </a-space>
          </a-card>
        </template>
      </a-card>
    </template>

    <template v-else>
      <a-card title="查询条件">
        <a-row :gutter="12">
          <a-col :xs="24" :sm="12" :lg="8">
            <a-input v-model:value="query.keyword" placeholder="姓名/手机号/身份证" />
          </a-col>
          <a-col v-if="!isCoach" :xs="24" :sm="12" :lg="8">
            <a-select
              v-model:value="query.drivingSchoolId"
              placeholder="按驾校筛选报名记录"
              style="width: 100%"
              allow-clear
              show-search
              option-filter-prop="label"
              :options="schoolOptions"
            />
          </a-col>
          <a-col :xs="24" :sm="12" :lg="8">
            <a-space>
              <a-button type="primary" @click="handleSearch">查询</a-button>
              <a-tag v-if="isCoach" color="blue">当前仅显示 {{ getSchoolName(currentUser?.drivingSchoolId) }} 的报名记录</a-tag>
            </a-space>
          </a-col>
        </a-row>
      </a-card>

      <a-card title="报名记录">
        <a-table
          :loading="recordsLoading"
          :columns="columns"
          :data-source="records"
          row-key="id"
          :pagination="{ current: query.page + 1, pageSize: query.size, total, onChange: (p, s) => { query.page = p - 1; query.size = s; loadEnrollmentRecords(); } }"
        />
      </a-card>
    </template>
  </a-space>
</template>
