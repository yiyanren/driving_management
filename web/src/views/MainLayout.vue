<script setup>
import { computed, h } from "vue";
import { useRouter, useRoute } from "vue-router";
import {
  DashboardOutlined,
  TeamOutlined,
  FileTextOutlined,
  BarChartOutlined,
  PlusOutlined,
  EnvironmentOutlined,
  ScheduleOutlined
} from "@ant-design/icons-vue";
import { useAuthStore } from "../stores/auth";

const router = useRouter();
const route = useRoute();
const auth = useAuthStore();

const allMenus = [
  { key: "/dashboard", icon: () => h(DashboardOutlined), label: "工作台", roles: ["管理员", "招生", "教练", "学员"] },
  { key: "/leads", icon: () => h(FileTextOutlined), label: "意向客户管理", roles: ["管理员", "招生"] },
  { key: "/students", icon: () => h(TeamOutlined), label: "学员管理", roles: ["管理员", "招生", "教练"] },
  { key: "/teaching", icon: () => h(ScheduleOutlined), label: "教学管理", roles: ["管理员", "教练"] },
  { key: "/exam", icon: () => h(PlusOutlined), label: "报考管理", roles: ["管理员", "招生"] },
  { key: "/map", icon: () => h(EnvironmentOutlined), label: "考场地图", roles: ["管理员", "招生", "教练", "学员"] },
  { key: "/reports", icon: () => h(BarChartOutlined), label: "报表中心", roles: ["管理员", "招生"] }
];

const menuItems = computed(() => allMenus.filter((m) => m.roles.includes(auth.role)));

const logout = () => {
  auth.logout();
  router.replace("/login");
};
</script>

<template>
  <a-layout class="app-layout">
    <a-layout-sider class="left-sider" :width="228">
      <div class="brand">驾考一点通</div>
      <a-menu :selectedKeys="[route.path]" theme="light" mode="inline" :items="menuItems" @click="({ key }) => router.push(key)" />
    </a-layout-sider>
    <a-layout>
      <a-layout-header class="top-header">
        <div class="header-title">驾校运营管理后台</div>
        <a-space>
          <span>{{ auth.user?.displayName || auth.user?.username }}</span>
          <a-tag color="blue">{{ auth.role }}</a-tag>
          <a-button @click="logout">退出登录</a-button>
        </a-space>
      </a-layout-header>
      <a-layout-content class="content-wrap">
        <router-view />
      </a-layout-content>
    </a-layout>
  </a-layout>
</template>
