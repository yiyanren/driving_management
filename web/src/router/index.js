import { createRouter, createWebHistory } from "vue-router";
import { useAuthStore } from "../stores/auth";
import LoginView from "../views/LoginView.vue";
import MainLayout from "../views/MainLayout.vue";
import DashboardView from "../views/DashboardView.vue";
import CoachDashboard from "../views/CoachDashboard.vue";
import StudentDashboard from "../views/StudentDashboard.vue";
import OnlineLearning from "../views/OnlineLearning.vue";
import ReservationView from "../views/ReservationView.vue";
import StudentExamView from "../views/StudentExamView.vue";
import EnrollmentView from "../views/EnrollmentView.vue";
import LeadsView from "../views/LeadsView.vue";
import StudentsView from "../views/StudentsView.vue";
import CoachesView from "../views/CoachesView.vue";
import DrivingSchoolsView from "../views/DrivingSchoolsView.vue";
import ProfileView from "../views/ProfileView.vue";
import TeachingView from "../views/TeachingView.vue";
import ExamView from "../views/ExamView.vue";
import MapView from "../views/MapView.vue";
import ReportsView from "../views/ReportsView.vue";

const routes = [
  { path: "/login", component: LoginView, meta: { public: true } },
  {
    path: "/",
    component: MainLayout,
    redirect: "/dashboard",
    children: [
      { path: "dashboard", component: DashboardView, meta: { roles: ["管理员", "招生"] } },
      { path: "coach-dashboard", component: CoachDashboard, meta: { roles: ["教练"] } },
      { path: "student-dashboard", component: StudentDashboard, meta: { roles: ["学员", "ROLE_STUDENT"] } },
      { path: "enrollment", component: EnrollmentView, meta: { roles: ["管理员", "教练", "学员", "ROLE_STUDENT"] } },
      { path: "learning/:mode", component: OnlineLearning, meta: { roles: ["学员", "ROLE_STUDENT"] } },
      { path: "reservation", component: ReservationView, meta: { roles: ["学员", "ROLE_STUDENT"] } },
      { path: "student-exam", component: StudentExamView, meta: { roles: ["学员", "ROLE_STUDENT"] } },
      { path: "leads", component: LeadsView, meta: { roles: ["管理员", "招生", "教练"] } },
      { path: "students", component: StudentsView, meta: { roles: ["管理员", "招生", "教练"] } },
      { path: "coaches", component: CoachesView, meta: { roles: ["管理员"] } },
      { path: "driving-schools", component: DrivingSchoolsView, meta: { roles: ["管理员"] } },
      { path: "profile", component: ProfileView, meta: { roles: ["管理员", "招生", "教练", "学员", "ROLE_STUDENT"] } },
      { path: "teaching", component: TeachingView, meta: { roles: ["管理员", "教练"] } },
      { path: "exam", component: ExamView, meta: { roles: ["管理员", "招生"] } },
      { path: "map", component: MapView, meta: { roles: ["管理员", "招生", "教练", "学员", "ROLE_STUDENT"] } },
      { path: "reports", component: ReportsView, meta: { roles: ["管理员", "招生"] } }
    ]
  }
];

const defaultPathByRole = (role) => {
  if (role === "管理员" || role === "招生") return "/dashboard";
  if (role === "教练") return "/coach-dashboard";
  if (role === "学员" || role === "ROLE_STUDENT") return "/student-dashboard";
  return "/login";
};

const router = createRouter({
  history: createWebHistory(),
  routes
});

router.beforeEach((to) => {
  const auth = useAuthStore();
  if (to.meta.public) {
    return true;
  }
  if (!auth.isLogin) {
    return "/login";
  }
  if (to.path === "/") {
    return defaultPathByRole(auth.role);
  }
  const roles = to.meta.roles || [];
  if (roles.length && !roles.includes(auth.role)) {
    return defaultPathByRole(auth.role);
  }
  return true;
});

export default router;
