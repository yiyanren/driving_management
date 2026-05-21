<script setup>
import { computed, onMounted, reactive, ref } from "vue";
import { message } from "ant-design-vue";
import { api } from "../api";

const students = ref([]);
const siteOptionsSource = ref([]);
const siteList = ref([]);
const siteTotal = ref(0);
const siteLoading = ref(false);
const sitePage = ref(1);
const siteSize = ref(10);
const siteKeyword = ref("");
const importing = ref(false);
const savingSite = ref(false);
const siteModalVisible = ref(false);

const examForm = reactive({
  studentId: undefined,
  subjectCode: "科目一",
  examSiteId: undefined,
  examDate: "",
  status: "已申请"
});

const siteForm = reactive({
  name: "",
  subjectType: "",
  vehicleTypes: "",
  address: ""
});

const subjectOptions = ["科目一", "科目二", "科目三", "科目四"].map((item) => ({
  label: item,
  value: item
}));

const siteColumns = [
  { title: "考场名称", dataIndex: "name", key: "name" },
  { title: "所属区域", dataIndex: "regionName", key: "regionName", width: 140 },
  { title: "考试科目", dataIndex: "subjectType", key: "subjectType", width: 180 },
  { title: "可考车型", dataIndex: "vehicleTypes", key: "vehicleTypes", width: 140 },
  { title: "地址", dataIndex: "address", key: "address" }
];

const studentOptions = computed(() =>
  students.value.map((student) => ({
    label: `${student.name} / ${student.phone}`,
    value: student.id
  }))
);

const siteOptions = computed(() =>
  siteOptionsSource.value.map((site) => ({
    label: `${site.name}${site.regionName ? ` / ${site.regionName}` : ""}`,
    value: site.id
  }))
);

const resetSiteForm = () => {
  Object.assign(siteForm, {
    name: "",
    subjectType: "",
    vehicleTypes: "",
    address: ""
  });
};

const loadBaseData = async () => {
  const [studentRes, siteRes] = await Promise.all([
    api.listStudents({ page: 0, size: 500 }),
    api.listExamSites({ page: 0, size: 500 })
  ]);
  if (studentRes.data.success) {
    students.value = studentRes.data.data.content || [];
  }
  if (siteRes.data.success) {
    siteOptionsSource.value = siteRes.data.data.content || [];
  }
};

const loadSiteList = async () => {
  siteLoading.value = true;
  try {
    const res = await api.listExamSites({
      page: sitePage.value - 1,
      size: siteSize.value,
      keyword: siteKeyword.value || undefined
    });
    if (!res.data.success) {
      return message.error(res.data.message || "考场列表加载失败");
    }
    siteList.value = res.data.data.content || [];
    siteTotal.value = res.data.data.totalElements || 0;
  } finally {
    siteLoading.value = false;
  }
};

const refreshSiteData = async () => {
  await Promise.all([loadBaseData(), loadSiteList()]);
};

const handleSearch = async () => {
  sitePage.value = 1;
  await loadSiteList();
};

const openAddSiteModal = () => {
  resetSiteForm();
  siteModalVisible.value = true;
};

const saveSite = async () => {
  if (!siteForm.name.trim()) {
    return message.warning("请输入考场名称");
  }
  if (!siteForm.address.trim()) {
    return message.warning("请输入考场地址");
  }
  savingSite.value = true;
  try {
    const res = await api.createExamSite({
      name: siteForm.name.trim(),
      subjectType: siteForm.subjectType.trim(),
      vehicleTypes: siteForm.vehicleTypes.trim(),
      address: siteForm.address.trim()
    });
    if (!res.data.success) {
      return message.error(res.data.message || "新增失败");
    }
    message.success("考场新增成功");
    siteModalVisible.value = false;
    resetSiteForm();
    await refreshSiteData();
  } finally {
    savingSite.value = false;
  }
};

const importSites = async (file) => {
  importing.value = true;
  try {
    const formData = new FormData();
    formData.append("file", file);
    const res = await api.importExamSites(formData);
    if (!res.data.success) {
      return message.error(res.data.message || "导入失败");
    }
    message.success(`导入完成：新增 ${res.data.data.imported} 条，更新 ${res.data.data.updated} 条`);
    await refreshSiteData();
  } finally {
    importing.value = false;
  }
  return false;
};

const createExam = async () => {
  if (!examForm.studentId) {
    return message.warning("请选择学员（姓名 + 手机号）");
  }
  if (!examForm.examSiteId) {
    return message.warning("请选择考场");
  }
  if (!examForm.examDate) {
    return message.warning("请选择考试日期");
  }
  const payload = {
    ...examForm,
    studentId: Number(examForm.studentId),
    examSiteId: Number(examForm.examSiteId)
  };
  const res = await api.createExamApplication(payload);
  if (!res.data.success) {
    return message.error(res.data.message || "创建失败");
  }
  message.success("报考申请提交成功");
};

onMounted(async () => {
  await refreshSiteData();
});
</script>

<template>
  <div class="view-container">
    <a-card title="报考申请" :bordered="false">
      <a-row :gutter="[12, 12]">
        <a-col :xs="24" :sm="12" :lg="6">
          <a-select
            v-model:value="examForm.studentId"
            :options="studentOptions"
            placeholder="学员姓名 + 手机号"
            show-search
            option-filter-prop="label"
          />
        </a-col>
        <a-col :xs="24" :sm="12" :lg="6">
          <a-select v-model:value="examForm.subjectCode" :options="subjectOptions" placeholder="考试科目" />
        </a-col>
        <a-col :xs="24" :sm="12" :lg="6">
          <a-select
            v-model:value="examForm.examSiteId"
            :options="siteOptions"
            placeholder="选择考场"
            show-search
            option-filter-prop="label"
          />
        </a-col>
        <a-col :xs="24" :sm="12" :lg="6">
          <a-date-picker style="width: 100%;" @change="(_, value) => examForm.examDate = value" />
        </a-col>
      </a-row>
      <a-button style="margin-top: 12px;" type="primary" @click="createExam">提交报考</a-button>
    </a-card>

    <a-card title="考场列表" class="table-card" :bordered="false">
      <template #extra>
        <a-space wrap>
          <a-input
            v-model:value="siteKeyword"
            placeholder="搜索考场名称/区域/地址"
            allow-clear
            style="width: 240px;"
            @pressEnter="handleSearch"
          />
          <a-button @click="handleSearch">搜索</a-button>
          <a-upload :show-upload-list="false" :before-upload="importSites">
            <a-button :loading="importing">上传考场信息表</a-button>
          </a-upload>
          <a-button type="primary" @click="openAddSiteModal">新增考场</a-button>
        </a-space>
      </template>

      <div class="table-tip">考场容量不再由系统手动定义，考场维护仅保留基础信息导入与新增。</div>

      <a-table
        :columns="siteColumns"
        :data-source="siteList"
        :loading="siteLoading"
        :pagination="{
          current: sitePage,
          pageSize: siteSize,
          total: siteTotal,
          onChange: (page, size) => {
            sitePage = page;
            siteSize = size;
            loadSiteList();
          }
        }"
        row-key="id"
        bordered
      />
    </a-card>

    <a-modal
      v-model:visible="siteModalVisible"
      title="新增考场"
      :confirm-loading="savingSite"
      @ok="saveSite"
      width="720px"
      :destroyOnClose="true"
    >
      <a-form layout="vertical">
        <a-row :gutter="16">
          <a-col :xs="24" :sm="12">
            <a-form-item label="考场名称" required>
              <a-input v-model:value="siteForm.name" placeholder="请输入考场名称" />
            </a-form-item>
          </a-col>
          <a-col :xs="24" :sm="12">
            <a-form-item label="考试科目">
              <a-input v-model:value="siteForm.subjectType" placeholder="如：科目一、科目四" />
            </a-form-item>
          </a-col>
          <a-col :xs="24" :sm="12">
            <a-form-item label="可考车型">
              <a-input v-model:value="siteForm.vehicleTypes" placeholder="如：C1、C2" />
            </a-form-item>
          </a-col>
          <a-col :xs="24">
            <a-form-item label="考场地址" required>
              <a-input v-model:value="siteForm.address" placeholder="请输入考场地址" />
            </a-form-item>
          </a-col>
        </a-row>
      </a-form>
    </a-modal>
  </div>
</template>

<style scoped>
.view-container {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.table-card :deep(.ant-card-body) {
  padding-top: 16px;
}

.table-tip {
  margin-bottom: 12px;
  color: #8c8c8c;
  font-size: 13px;
}
</style>
