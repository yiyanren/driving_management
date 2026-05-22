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
  ScheduleOutlined,
  ReadOutlined,
  CalendarOutlined,
  FormOutlined
} from "@ant-design/icons-vue";
import { useAuthStore } from "../stores/auth";

const router = useRouter();
const route = useRoute();
const auth = useAuthStore();

const allMenus = [
  { key: "/dashboard", icon: () => h(DashboardOutlined), label: "工作台", roles: ["管理员", "招生"] },
  { key: "/coach-dashboard", icon: () => h(DashboardOutlined), label: "我的主页", roles: ["教练"] },
  { key: "/student-dashboard", icon: () => h(DashboardOutlined), label: "我的主页", roles: ["学员", "ROLE_STUDENT"] },
  {
    key: "online-learning",
    icon: () => h(ReadOutlined),
    label: "在线学习",
    roles: ["学员", "ROLE_STUDENT"],
    children: [
      { key: "/learning/order", label: "顺序练习", roles: ["学员", "ROLE_STUDENT"] },
      { key: "/learning/mock", label: "模拟考试", roles: ["学员", "ROLE_STUDENT"] },
      { key: "/learning/mistakes", label: "错题本", roles: ["学员", "ROLE_STUDENT"] }
    ]
  },
  { key: "/enrollment", icon: () => h(FormOutlined), label: "报名管理", roles: ["管理员", "教练", "学员", "ROLE_STUDENT"] },
  { key: "/reservation", icon: () => h(CalendarOutlined), label: "预约培训", roles: ["学员", "ROLE_STUDENT"] },
  { key: "/student-exam", icon: () => h(PlusOutlined), label: "申请考试", roles: ["学员", "ROLE_STUDENT"] },
  {
    key: "personnel-management",
    icon: () => h(TeamOutlined),
    label: "人员管理",
    roles: ["管理员", "招生", "教练"],
    children: [
      { key: "/leads", icon: () => h(FileTextOutlined), label: "意向客户管理", roles: ["管理员", "招生", "教练"] },
      { key: "/students", icon: () => h(TeamOutlined), label: "学员管理", roles: ["管理员", "招生", "教练"] },
      { key: "/coaches", icon: () => h(TeamOutlined), label: "教练管理", roles: ["管理员"] },
      { key: "/driving-schools", icon: () => h(EnvironmentOutlined), label: "驾校管理", roles: ["管理员"] }
    ]
  },
  { key: "/teaching", icon: () => h(ScheduleOutlined), label: "教学管理", roles: ["管理员", "教练"] },
  { key: "/exam", icon: () => h(PlusOutlined), label: "考场预约查询", roles: ["管理员", "招生"] },
  { key: "/map", icon: () => h(EnvironmentOutlined), label: "考场地图", roles: ["管理员", "招生", "教练", "学员", "ROLE_STUDENT"] },
  { key: "/reports", icon: () => h(BarChartOutlined), label: "报表中心", roles: ["管理员", "招生"] }
];

const menuItems = computed(() => {
  const currentRole = auth.role === "ROLE_STUDENT" ? "学员" : auth.role;
  return allMenus
    .filter((m) => m.roles.includes(currentRole))
    .map((m) => {
      if (m.children) {
        return {
          ...m,
          children: m.children.filter((c) => c.roles.includes(currentRole))
        };
      }
      return m;
    });
});

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
        <a-dropdown>
          <a-space style="cursor: pointer;">
            <span>{{ auth.user?.displayName || auth.user?.username }}</span>
            <a-tag color="blue">{{ auth.role === 'ROLE_STUDENT' ? '学员' : auth.role }}</a-tag>
          </a-space>
          <template #overlay>
            <a-menu>
              <a-menu-item key="profile" @click="router.push('/profile')">
                个人中心
              </a-menu-item>
              <a-menu-item key="logout" @click="logout">
                退出登录
              </a-menu-item>
            </a-menu>
          </template>
        </a-dropdown>
      </a-layout-header>
      <a-layout-content class="content-wrap">
        <router-view />
      </a-layout-content>
    </a-layout>
  </a-layout>
</template>
