<script setup>
import { reactive, ref } from "vue";
import { useRouter } from "vue-router";
import { message } from "ant-design-vue";
import { api } from "../api";
import { useAuthStore } from "../stores/auth";

const router = useRouter();
const auth = useAuthStore();
const loading = ref(false);
const form = reactive({ username: "admin", password: "admin123" });
const activeTab = ref("login");
const regLoading = ref(false);
const regFormRef = ref();
const regForm = reactive({
  username: "",
  password: "",
  name: "",
  phone: "",
  idCard: ""
});

const regRules = {
  username: [{ required: true, message: "请输入账号" }],
  password: [{ required: true, message: "请输入密码" }],
  name: [{ required: true, message: "请输入姓名" }],
  phone: [
    { required: true, message: "请输入手机号" },
    { pattern: /^1\d{10}$/, message: "手机号格式不正确" }
  ],
  idCard: [{ required: true, message: "请输入身份证号" }]
};

const defaultPathByRole = (role) => {
  if (role === "管理员" || role === "招生") return "/dashboard";
  if (role === "教练") return "/coach-dashboard";
  if (role === "学员" || role === "ROLE_STUDENT") return "/student-dashboard";
  return "/dashboard";
};

const submit = async () => {
  loading.value = true;
  try {
    const res = await api.login(form);
    if (!res.data.success) {
      message.error(res.data.message || "登录失败");
      return;
    }
    auth.setLogin(res.data.data);
    message.success("登录成功");
    router.replace(defaultPathByRole(res.data.data.role));
  } catch (e) {
    message.error(e?.response?.data?.message || e.message || "登录失败");
  } finally {
    loading.value = false;
  }
};
const submitReg = () => {
  regFormRef.value.validate().then(async () => {
    regLoading.value = true;
    try {
      const res = await api.register(regForm);
      if (res.data.success) {
        message.success("注册成功，请使用新账号登录");
        activeTab.value = "login";
        form.username = regForm.username;
        form.password = regForm.password; 
        // 重置注册表单
        regFormRef.value.resetFields();
        // 自动触发一次登录操作
        submit();
      } else {
        message.error(res.data.message || "注册失败");
      }
    } catch (e) {
      message.error(e?.response?.data?.message || "注册失败，账号可能已被占用");
    } finally {
      regLoading.value = false;
    }
  });
};
</script>

<template>
  <div class="login-wrap">
    <a-card class="login-card">
      <h2 style="text-align: center; margin-bottom: 24px;">驾考一点通</h2>
      <a-alert
        v-if="activeTab === 'login'"
        type="info"
        show-icon
        message="演示账号：admin/admin123、sales/sales123、coach/coach123、student/student123"
        style="margin-bottom: 12px;"
      />
      <a-tabs v-model:activeKey="activeTab" centered>
        <a-tab-pane key="login" tab="账号登录">
          <a-form layout="vertical">
            <a-form-item label="用户名">
              <a-input v-model:value="form.username" />
            </a-form-item>
            <a-form-item label="密码">
              <a-input-password v-model:value="form.password" @keyup.enter="submit" />
            </a-form-item>
            <a-button block type="primary" :loading="loading" @click="submit">登录</a-button>
          </a-form>
        </a-tab-pane>
        <a-tab-pane key="register" tab="学员注册">
          <a-form ref="regFormRef" :model="regForm" :rules="regRules" layout="vertical">
            <a-form-item label="账号" name="username">
              <a-input v-model:value="regForm.username" placeholder="请输入英文或数字组合" />
            </a-form-item>
            <a-form-item label="密码" name="password">
              <a-input-password v-model:value="regForm.password" />
            </a-form-item>
            <a-form-item label="真实姓名" name="name">
              <a-input v-model:value="regForm.name" />
            </a-form-item>
            <a-form-item label="手机号" name="phone">
              <a-input v-model:value="regForm.phone" />
            </a-form-item>
            <a-form-item label="身份证号" name="idCard">
              <a-input v-model:value="regForm.idCard" />
            </a-form-item>
            <a-button block type="primary" :loading="regLoading" @click="submitReg">注册</a-button>
          </a-form>
        </a-tab-pane>
      </a-tabs>
    </a-card>
  </div>
</template>
