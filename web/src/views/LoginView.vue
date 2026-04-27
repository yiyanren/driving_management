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

const defaultPathByRole = (role) => {
  if (role === "管理员" || role === "招生") return "/dashboard";
  if (role === "教练") return "/teaching";
  if (role === "学员") return "/map";
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
</script>

<template>
  <div class="login-wrap">
    <a-card title="驾考一点通" class="login-card">
      <a-alert
        type="info"
        show-icon
        message="演示账号：admin/admin123、sales/sales123、coach/coach123、student/student123"
        style="margin-bottom: 12px;"
      />
      <a-form layout="vertical">
        <a-form-item label="用户名">
          <a-input v-model:value="form.username" />
        </a-form-item>
        <a-form-item label="密码">
          <a-input-password v-model:value="form.password" @keyup.enter="submit" />
        </a-form-item>
        <a-button block type="primary" :loading="loading" @click="submit">登录</a-button>
      </a-form>
    </a-card>
  </div>
</template>
