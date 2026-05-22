<script setup>
import { onMounted, reactive, ref } from "vue";
import { message } from "ant-design-vue";
import { api } from "../api";

const queryUrl = "https://hb.122.gov.cn/web/html/?type=1&ticket=NIL&code=E_A_1023#/nView/exam/limitpub";

const siteList = ref([]);
const siteTotal = ref(0);
const siteLoading = ref(false);
const sitePage = ref(1);
const siteSize = ref(10);
const siteKeyword = ref("");
const importing = ref(false);
const savingSite = ref(false);
const siteModalVisible = ref(false);

const siteForm = reactive({
  name: "",
  subjectType: "",
  vehicleTypes: "",
  address: ""
});

const siteColumns = [
  { title: "考场名称", dataIndex: "name", key: "name" },
  { title: "所属区域", dataIndex: "regionName", key: "regionName", width: 140 },
  { title: "考试科目", dataIndex: "subjectType", key: "subjectType", width: 180 },
  { title: "可考车型", dataIndex: "vehicleTypes", key: "vehicleTypes", width: 140 },
  { title: "地址", dataIndex: "address", key: "address" }
];

const resetSiteForm = () => {
  Object.assign(siteForm, {
    name: "",
    subjectType: "",
    vehicleTypes: "",
    address: ""
  });
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

const handleSearch = async () => {
  sitePage.value = 1;
  await loadSiteList();
};

const handleTableChange = async (page, size) => {
  sitePage.value = page;
  siteSize.value = size;
  await loadSiteList();
};

const openQueryPage = () => {
  window.open(queryUrl, "_blank");
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
    await loadSiteList();
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
    await loadSiteList();
  } finally {
    importing.value = false;
  }
  return false;
};

onMounted(async () => {
  await loadSiteList();
});
</script>

<template>
  <div class="view-container">
    <a-card title="考场预约查询" :bordered="false">
      <div class="query-panel">
        <div>
          <div class="query-title">官方考试预约查询入口</div>
          
        </div>
        <a-button type="primary" @click="openQueryPage">前往查询</a-button>
      </div>
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


      <a-table
        :columns="siteColumns"
        :data-source="siteList"
        :loading="siteLoading"
        :pagination="{
          current: sitePage,
          pageSize: siteSize,
          total: siteTotal,
          onChange: handleTableChange
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

.query-panel {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
}

.query-title {
  font-size: 16px;
  font-weight: 600;
  color: #262626;
}

.query-description {
  margin-top: 8px;
  color: #595959;
  line-height: 1.6;
}

.table-tip {
  margin-bottom: 12px;
  color: #8c8c8c;
  font-size: 13px;
}

@media (max-width: 768px) {
  .query-panel {
    flex-direction: column;
    align-items: flex-start;
  }
}
</style>
