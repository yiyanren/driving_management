<script setup>
import { ref, onMounted } from "vue";
import { message } from "ant-design-vue";
import { api } from "../api";
import { useAuthStore } from "../stores/auth";

const auth = useAuthStore();
const activeKey = ref("1");

// Profile form
const profileLoading = ref(false);
const profileRef = ref();
const profileData = ref({
  name: "",
  phone: "",
  idCard: ""
});
const profileRules = {
  name: [{ required: true, message: "请输入姓名" }],
  phone: [{ required: true, message: "请输入手机号" }, { pattern: /^1\d{10}$/, message: "手机号格式不正确" }],
  idCard: [{ required: true, message: "请输入身份证号" }]
};

// Password form
const pwdLoading = ref(false);
const pwdRef = ref();
const pwdData = ref({
  oldPassword: "",
  newPassword: "",
  confirmPassword: ""
});

const checkConfirm = async (_rule, value) => {
  if (value === "") {
    return Promise.reject("请再次输入新密码");
  } else if (value !== pwdData.value.newPassword) {
    return Promise.reject("两次输入的密码不一致!");
  } else {
    return Promise.resolve();
  }
};

const pwdRules = {
  oldPassword: [{ required: true, message: "请输入旧密码" }],
  newPassword: [{ required: true, message: "请输入新密码" }, { min: 6, message: "密码长度不能小于6位" }],
  confirmPassword: [{ required: true, validator: checkConfirm, trigger: "change" }]
};

const loadProfile = async () => {
  if (auth.role === "学员" || auth.role === "ROLE_STUDENT") {
    try {
      const res = await api.getStudentMe();
      if (res.data.success) {
        const { name, phone, idCard } = res.data.data;
        profileData.value = { name, phone, idCard };
      }
    } catch (e) {
      message.error("获取个人信息失败");
    }
  }
};

const handleUpdateProfile = () => {
  profileRef.value.validate().then(async () => {
    profileLoading.value = true;
    try {
      if (auth.role === "学员" || auth.role === "ROLE_STUDENT") {
        await api.updateStudentMe(profileData.value);
        message.success("个人信息更新成功");
        // 更新成功后，重新从数据库加载最新信息以确认写入
        await loadProfile();
      }
    } catch (e) {
      message.error("更新失败");
    } finally {
      profileLoading.value = false;
    }
  });
};

const handleUpdatePassword = () => {
  pwdRef.value.validate().then(async () => {
    pwdLoading.value = true;
    try {
      await api.updateMyPassword({
        oldPassword: pwdData.value.oldPassword,
        newPassword: pwdData.value.newPassword
      });
      message.success("密码修改成功，请重新登录");
      setTimeout(() => {
        auth.logout();
        location.href = "/login";
      }, 1500);
    } catch (e) {
      message.error(e.response?.data?.message || "密码修改失败");
    } finally {
      pwdLoading.value = false;
    }
  });
};

const handleTabChange = (key) => {
  if (key === "1") {
    loadProfile();
  }
};

onMounted(() => {
  loadProfile();
});
</script>

<template>
  <div class="view-container">
    <a-tabs v-model:activeKey="activeKey" @change="handleTabChange">
      <a-tab-pane key="1" tab="基本信息">
        <div style="max-width: 500px; margin-top: 24px;">
          <template v-if="auth.role === '学员' || auth.role === 'ROLE_STUDENT'">
            <a-form ref="profileRef" :model="profileData" :rules="profileRules" layout="vertical">
              <a-form-item label="姓名" name="name">
                <a-input v-model:value="profileData.name" />
              </a-form-item>
              <a-form-item label="手机号" name="phone">
                <a-input v-model:value="profileData.phone" />
              </a-form-item>
              <a-form-item label="身份证" name="idCard">
                <a-input v-model:value="profileData.idCard" />
              </a-form-item>
              <a-form-item>
                <a-button type="primary" :loading="profileLoading" @click="handleUpdateProfile">保存更改</a-button>
              </a-form-item>
            </a-form>
          </template>
          <template v-else>
            <a-descriptions bordered column="1">
              <a-descriptions-item label="账号">{{ auth.user?.username }}</a-descriptions-item>
              <a-descriptions-item label="角色">{{ auth.role === 'ROLE_STUDENT' ? '学员' : auth.role }}</a-descriptions-item>
              <a-descriptions-item label="显示名称">{{ auth.user?.displayName || '-' }}</a-descriptions-item>
            </a-descriptions>
            <div style="margin-top: 16px; color: #8c8c8c;">非学员角色暂不支持修改基本信息，请联系管理员。</div>
          </template>
        </div>
      </a-tab-pane>

      <a-tab-pane key="2" tab="安全设置">
        <div style="max-width: 500px; margin-top: 24px;">
          <a-form ref="pwdRef" :model="pwdData" :rules="pwdRules" layout="vertical">
            <a-form-item label="旧密码" name="oldPassword">
              <a-input-password v-model:value="pwdData.oldPassword" placeholder="请输入当前密码" />
            </a-form-item>
            <a-form-item label="新密码" name="newPassword">
              <a-input-password v-model:value="pwdData.newPassword" placeholder="请输入新密码（至少6位）" />
            </a-form-item>
            <a-form-item label="确认新密码" name="confirmPassword">
              <a-input-password v-model:value="pwdData.confirmPassword" placeholder="请再次输入新密码" />
            </a-form-item>
            <a-form-item>
              <a-button type="primary" :loading="pwdLoading" @click="handleUpdatePassword">修改密码</a-button>
            </a-form-item>
          </a-form>
        </div>
      </a-tab-pane>
    </a-tabs>
  </div>
</template>

<style scoped>
.view-container {
  padding: 24px;
  background: #fff;
  border-radius: 8px;
  min-height: calc(100vh - 112px);
}
</style>