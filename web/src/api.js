import axios from "axios";
import { message } from "ant-design-vue";

const http = axios.create({
  baseURL: "/api",
  timeout: 10000
});

http.interceptors.request.use((config) => {
  const token = localStorage.getItem("token");
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

http.interceptors.response.use(
  (res) => res,
  (err) => {
    const status = err?.response?.status;
    if (status === 401) {
      localStorage.removeItem("token");
      localStorage.removeItem("user");
      message.error("登录已失效，请重新登录");
      location.href = "/login";
    }
    if (status === 403) {
      message.warning("当前账号无权限执行该操作");
    }
    return Promise.reject(err);
  }
);

export const api = {
  login: (data) => http.post("/auth/login", data),
  register: (data) => http.post("/auth/register", data),

  getMe: () => http.get("/users/me"),
  updateMyPassword: (data) => http.put("/users/me/password", data),
  getStudentMe: () => http.get("/students/me"),
  updateStudentMe: (data) => http.put("/students/me", data),
  submitStudentEnrollment: (drivingSchoolId) => http.post("/students/me/enroll", null, { params: { drivingSchoolId } }),
  listEnrollmentRecords: (params) => http.get("/students/enrollments", { params }),

  listDrivingSchools: (params) => http.get("/driving-schools", { params }),
  listAllDrivingSchools: () => http.get("/driving-schools/all"),
  createDrivingSchool: (data) => http.post("/driving-schools", data),
  updateDrivingSchool: (id, data) => http.put(`/driving-schools/${id}`, data),
  deleteDrivingSchool: (id) => http.delete(`/driving-schools/${id}`),

  listUsers: (params) => http.get("/users", { params }),
  createUser: (data) => http.post("/users", data),
  updateUser: (id, data) => http.put(`/users/${id}`, data),
  deleteUser: (id) => http.delete(`/users/${id}`),

  listLeads: (params) => http.get("/leads", { params }),
  createLead: (data) => http.post("/leads", data),
  convertLead: (id, idCard) => http.post(`/leads/${id}/convert`, null, { params: { idCard } }),
  listStudents: (params) => http.get("/students", { params }),
  getStudent: (id) => http.get(`/students/${id}`),
  updateStudent: (id, data) => http.put(`/students/${id}`, data),
  deleteStudent: (id) => http.delete(`/students/${id}`),
  listExamSites: (params) => http.get("/exam-sites", { params }),
  createExamSite: (data) => http.post("/exam-sites", data),
  importExamSites: (formData, defaultCapacity) => http.post("/exam-sites/import", formData, {
    params: defaultCapacity === undefined ? undefined : { defaultCapacity },
    headers: { "Content-Type": "multipart/form-data" }
  }),
  importLocalExamSites: (defaultCapacity) => http.post("/exam-sites/import-local", null, {
    params: { defaultCapacity }
  }),
  saveExamSiteSchedule: (data) => http.post("/exam-sites/schedules", data),
  listRealtimeExamSites: (params) => http.get("/exam-sites/realtime", { params }),
  getExamSiteTrend: (params) => http.get("/exam-sites/trend", { params }),
  listPlans: (params) => http.get("/teaching/plans", { params }),
  createPlan: (data) => http.post("/teaching/plans", data),
  updatePlanStatus: (id, status) => http.patch(`/teaching/plans/${id}/status`, null, { params: { status } }),
  listRecords: (params) => http.get("/teaching/records", { params }),
  createRecord: (data) => http.post("/teaching/records", data),
  getProgress: (studentId) => http.get("/teaching/progress", { params: { studentId } }),
  listExamApplications: (params) => http.get("/exam-applications", { params }),
  createExamApplication: (data) => http.post("/exam-applications", data),
  updateExamStatus: (id, status) => http.patch(`/exam-applications/${id}/status`, null, { params: { status } }),
  reportOverview: (params) => http.get("/reports/overview", { params }),
  reportFunnel: (params) => http.get("/reports/funnel", { params }),
  exportOverviewCsv: (params) => http.get("/reports/export/overview", { params, responseType: "blob" }),

  // 聚合数据 - 题库接口
  getQuestions: (params) => http.get("/questions", { params }),
};
