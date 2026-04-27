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

  listLeads: (params) => http.get("/leads", { params }),
  createLead: (data) => http.post("/leads", data),
  convertLead: (id, idCard) => http.post(`/leads/${id}/convert`, null, { params: { idCard } }),
  listStudents: (params) => http.get("/students", { params }),
  updateStudent: (id, data) => http.put(`/students/${id}`, data),
  deleteStudent: (id) => http.delete(`/students/${id}`),
  listExamSites: (params) => http.get("/exam-sites", { params }),
  createExamSite: (data) => http.post("/exam-sites", data),
  listPlans: (params) => http.get("/teaching/plans", { params }),
  createPlan: (data) => http.post("/teaching/plans", data),
  listRecords: (params) => http.get("/teaching/records", { params }),
  createRecord: (data) => http.post("/teaching/records", data),
  getProgress: (studentId) => http.get("/teaching/progress", { params: { studentId } }),
  listExamApplications: (params) => http.get("/exam-applications", { params }),
  createExamApplication: (data) => http.post("/exam-applications", data),
  updateExamStatus: (id, status) => http.patch(`/exam-applications/${id}/status`, null, { params: { status } }),
  reportOverview: (params) => http.get("/reports/overview", { params }),
  reportFunnel: (params) => http.get("/reports/funnel", { params }),
  exportOverviewCsv: (params) => http.get("/reports/export/overview", { params, responseType: "blob" })
};
