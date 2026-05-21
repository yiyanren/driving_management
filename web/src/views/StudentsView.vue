<script setup>
import { computed, onMounted, reactive, ref } from "vue";
import { message } from "ant-design-vue";
import { api } from "../api";

const loading = ref(false);
const list = ref([]);
const total = ref(0);
const query = reactive({ page: 0, size: 10, keyword: "", status: "" });

const editVisible = ref(false);
const editLoading = ref(false);
const editForm = reactive({
  id: null,
  name: "",
  phone: "",
  idCard: "",
  status: "",
  drivingSchoolId: undefined,
  subjectOnePassed: false,
  subjectTwoPassed: false,
  subjectThreePassed: false,
  subjectFourPassed: false
});

const addVisible = ref(false);
const addLoading = ref(false);
const addType = ref("convert"); // 'convert' or 'direct'
const addForm = reactive({
  leadId: undefined,
  name: "",
  phone: "",
  source: "",
  owner: "",
  idCard: "",
  drivingSchoolId: undefined
});
const unconvertedLeads = ref([]);
const schools = ref([]);

const subjectCheckboxDisabled = computed(() => ({
  subjectOnePassed: editForm.subjectTwoPassed || editForm.subjectThreePassed || editForm.subjectFourPassed,
  subjectTwoPassed: !editForm.subjectOnePassed || editForm.subjectThreePassed || editForm.subjectFourPassed,
  subjectThreePassed: !editForm.subjectTwoPassed || editForm.subjectFourPassed,
  subjectFourPassed: !editForm.subjectThreePassed
}));

const columns = [
  { title: "姓名", dataIndex: "name", key: "name" },
  { title: "手机号", dataIndex: "phone", key: "phone" },
  { title: "身份证", dataIndex: "idCard", key: "idCard" },
  { title: "所属驾校", dataIndex: "schoolName", key: "schoolName" },
  { title: "状态", dataIndex: "status", key: "status" },
  { title: "操作", key: "action", width: 180 }
];

const loadData = async () => {
  loading.value = true;
  try {
    const res = await api.listStudents(query);
    if (res.data.success) {
      const students = res.data.data.content;
      
      const mappedStudents = students.map(student => {
        const school = schools.value.find(s => s.id === student.drivingSchoolId);
        return {
          ...student,
          schoolName: school ? school.name : '未分配'
        };
      });
      
      // 前端进行分组排序：先按 drivingSchoolId 排序，再按 createdAt（或 id）排序
      mappedStudents.sort((a, b) => {
        const schoolA = a.drivingSchoolId || 0;
        const schoolB = b.drivingSchoolId || 0;
        if (schoolA !== schoolB) {
          return schoolA - schoolB; // 按驾校 ID 分类聚拢
        }
        return b.id - a.id; 
      });
      
      list.value = mappedStudents;
      total.value = res.data.data.totalElements;
    }
  } finally {
    loading.value = false;
  }
};

const loadUnconvertedLeads = async () => {
  try {
    const res = await api.listLeads({ page: 0, size: 500 });
    if (res.data.success) {
      // 过滤掉已经是"已转学员"状态的线索
      unconvertedLeads.value = (res.data.data.content || []).filter(lead => lead.status !== '已转学员');
    }
  } catch (e) {
    console.error("获取意向客户失败", e);
  }
};

const openAdd = async () => {
  addType.value = "convert";
  Object.assign(addForm, { leadId: undefined, name: "", phone: "", source: "", owner: "", idCard: "" });
  await loadUnconvertedLeads();
  addVisible.value = true;
};

const submitAdd = async () => {
  if (!addForm.idCard) return message.warning("请填写身份证号");

  addLoading.value = true;
  try {
    let leadIdToConvert = addForm.leadId;

    if (addType.value === "direct") {
      if (!addForm.name || !addForm.phone) {
        addLoading.value = false;
        return message.warning("请填写姓名和手机号");
      }
      // 直接新增时，先创建意向客户
      const createRes = await api.createLead({
        name: addForm.name,
        phone: addForm.phone,
        source: addForm.source || "直接报名",
        owner: addForm.owner || "系统",
        status: "新建"
      });
      if (!createRes.data.success) {
        addLoading.value = false;
        return message.error(createRes.data.message || "创建意向客户失败");
      }
      leadIdToConvert = createRes.data.data.id;
    } else {
      if (!leadIdToConvert) {
        addLoading.value = false;
        return message.warning("请选择意向客户");
      }
    }

    // 将意向客户转为学员
    const convertRes = await api.convertLead(leadIdToConvert, addForm.idCard);
    if (!convertRes.data.success) {
      addLoading.value = false;
      return message.error(convertRes.data.message || "转化学员失败");
    }
    
    // 如果选择了驾校，更新学员的驾校ID
    if (addForm.drivingSchoolId) {
       const studentId = convertRes.data.data.id;
       const studentData = convertRes.data.data;
       studentData.drivingSchoolId = addForm.drivingSchoolId;
       await api.updateStudent(studentId, studentData);
    }

    message.success("新增学员成功");
    addVisible.value = false;
    loadData();
  } catch (e) {
    message.error(e?.response?.data?.message || "操作失败");
  } finally {
    addLoading.value = false;
  }
};

const applyEditForm = (student) => {
  editForm.id = student.id;
  editForm.name = student.name;
  editForm.phone = student.phone;
  editForm.idCard = student.idCard;
  editForm.status = student.status;
  editForm.drivingSchoolId = student.drivingSchoolId;
  editForm.subjectOnePassed = Boolean(student.subjectOnePassed);
  editForm.subjectTwoPassed = Boolean(student.subjectTwoPassed);
  editForm.subjectThreePassed = Boolean(student.subjectThreePassed);
  editForm.subjectFourPassed = Boolean(student.subjectFourPassed);
};

const openEdit = async (row) => {
  try {
    const res = await api.getStudent(row.id);
    if (res.data.success) {
      applyEditForm(res.data.data);
    } else {
      applyEditForm(row);
    }
  } catch (e) {
    applyEditForm(row);
    message.warning("获取学员最新信息失败，已使用列表数据打开编辑");
  }
  editVisible.value = true;
};

const submitEdit = async () => {
  if (!editForm.id) return;
  editLoading.value = true;
  try {
    const payload = {
      name: editForm.name,
      phone: editForm.phone,
      idCard: editForm.idCard,
      status: editForm.status,
      drivingSchoolId: editForm.drivingSchoolId,
      subjectOnePassed: editForm.subjectOnePassed,
      subjectTwoPassed: editForm.subjectTwoPassed,
      subjectThreePassed: editForm.subjectThreePassed,
      subjectFourPassed: editForm.subjectFourPassed
    };
    const res = await api.updateStudent(editForm.id, payload);
    if (!res.data.success) return message.error(res.data.message || "更新失败");
    applyEditForm(res.data.data);
    message.success("学员信息更新成功");
    editVisible.value = false;
    await loadData();
  } catch (e) {
    message.error(e?.response?.data?.message || "保存失败，请确认后端已重启并已同步最新字段");
  } finally {
    editLoading.value = false;
  }
};

const removeStudent = async (row) => {
  const res = await api.deleteStudent(row.id);
  if (!res.data.success) return message.error(res.data.message || "删除失败");
  message.success("学员删除成功");
  loadData();
};

onMounted(() => {
  loadData();
  api.listAllDrivingSchools().then(res => {
    if (res.data.success) {
      schools.value = res.data.data;
    }
  }).catch(() => {});
});
</script>

<template>
  <a-space direction="vertical" style="display:flex;" :size="16">
    <a-card title="查询条件">
      <a-row :gutter="12">
        <a-col :xs="24" :sm="12" :lg="6"><a-input v-model:value="query.keyword" placeholder="姓名/手机号/身份证" /></a-col>
        <a-col :xs="24" :sm="12" :lg="6"><a-input v-model:value="query.status" placeholder="状态" /></a-col>
        <a-col :xs="24" :sm="12" :lg="6">
          <a-select 
            v-model:value="query.drivingSchoolId" 
            placeholder="按驾校归类筛选" 
            style="width: 100%"
            allowClear
            :options="schools.map(s => ({ label: s.name, value: s.id }))"
          />
        </a-col>
        <a-col :xs="24" :sm="12" :lg="6">
          <a-space>
            <a-button type="primary" @click="query.page=0;loadData()">查询</a-button>
            <a-button type="primary" style="background:#52c41a;border-color:#52c41a;" @click="openAdd">新增学员</a-button>
          </a-space>
        </a-col>
      </a-row>
    </a-card>
    <a-card title="学员列表">
      <a-table
        :loading="loading"
        :columns="columns"
        :data-source="list"
        row-key="id"
        :pagination="{ current: query.page + 1, pageSize: query.size, total, onChange: (p,s)=>{ query.page=p-1; query.size=s; loadData(); } }"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'action'">
            <a-space>
              <a-button size="small" style="background:#e6f4ff;border-color:#91caff;color:#0958d9;" @click="openEdit(record)">编辑</a-button>
              <a-popconfirm title="确认删除该学员吗？" ok-text="确认" cancel-text="取消" @confirm="removeStudent(record)">
                <a-button size="small" style="background:#fff1f0;border-color:#ffa39e;color:#cf1322;">删除</a-button>
              </a-popconfirm>
            </a-space>
          </template>
        </template>
      </a-table>
    </a-card>
    <a-modal v-model:open="editVisible" title="编辑学员" :confirm-loading="editLoading" @ok="submitEdit">
      <a-space direction="vertical" style="display:flex;">
        <a-input v-model:value="editForm.name" placeholder="姓名" />
        <a-input v-model:value="editForm.phone" placeholder="手机号" />
        <a-input v-model:value="editForm.idCard" placeholder="身份证号" />
        <a-input v-model:value="editForm.status" placeholder="状态" />
        <a-select 
          v-model:value="editForm.drivingSchoolId" 
          placeholder="请选择所属驾校" 
          style="width: 100%"
          show-search
          option-filter-prop="label"
          :options="schools.map(s => ({ label: s.name, value: s.id }))"
          allowClear
        />
        <a-card size="small" title="考试科目进度">
          <a-space direction="vertical" style="display:flex;">
            <a-checkbox v-model:checked="editForm.subjectOnePassed" :disabled="subjectCheckboxDisabled.subjectOnePassed">科目一</a-checkbox>
            <a-checkbox v-model:checked="editForm.subjectTwoPassed" :disabled="subjectCheckboxDisabled.subjectTwoPassed">科目二</a-checkbox>
            <a-checkbox v-model:checked="editForm.subjectThreePassed" :disabled="subjectCheckboxDisabled.subjectThreePassed">科目三</a-checkbox>
            <a-checkbox v-model:checked="editForm.subjectFourPassed" :disabled="subjectCheckboxDisabled.subjectFourPassed">科目四</a-checkbox>
            <a-typography-text type="secondary">
              规则：必须按科目一到科目四顺序勾选，后续科目已勾选时不可取消前置科目。
            </a-typography-text>
          </a-space>
        </a-card>
      </a-space>
    </a-modal>

    <a-modal v-model:open="addVisible" title="新增学员" :confirm-loading="addLoading" @ok="submitAdd">
      <a-tabs v-model:activeKey="addType" centered>
        <a-tab-pane key="convert" tab="从未转化意向客户选择">
          <a-space direction="vertical" style="display:flex; margin-top: 16px;">
            <a-select 
              v-model:value="addForm.leadId" 
              placeholder="请选择意向客户" 
              style="width: 100%"
              show-search
              option-filter-prop="label"
              :options="unconvertedLeads.map(l => ({ label: `${l.name} - ${l.phone}`, value: l.id }))"
            />
            <a-input v-model:value="addForm.idCard" placeholder="请输入身份证号 (必填)" />
            <a-select 
              v-model:value="addForm.drivingSchoolId" 
              placeholder="请选择所属驾校" 
              style="width: 100%"
              show-search
              option-filter-prop="label"
              :options="schools.map(s => ({ label: s.name, value: s.id }))"
              allowClear
            />
          </a-space>
        </a-tab-pane>
        <a-tab-pane key="direct" tab="直接新增学员">
          <a-space direction="vertical" style="display:flex; margin-top: 16px;">
            <a-input v-model:value="addForm.name" placeholder="姓名 (必填)" />
            <a-input v-model:value="addForm.phone" placeholder="手机号 (必填)" />
            <a-input v-model:value="addForm.source" placeholder="来源 (如: 传单, 网络推广)" />
            <a-input v-model:value="addForm.owner" placeholder="招生人员" />
            <a-input v-model:value="addForm.idCard" placeholder="请输入身份证号 (必填)" />
            <a-select 
              v-model:value="addForm.drivingSchoolId" 
              placeholder="请选择所属驾校" 
              style="width: 100%"
              show-search
              option-filter-prop="label"
              :options="schools.map(s => ({ label: s.name, value: s.id }))"
              allowClear
            />
          </a-space>
        </a-tab-pane>
      </a-tabs>
    </a-modal>
  </a-space>
</template>
