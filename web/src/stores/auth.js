import { defineStore } from "pinia";

export const useAuthStore = defineStore("auth", {
  state: () => ({
    token: localStorage.getItem("token") || "",
    user: JSON.parse(localStorage.getItem("user") || "null")
  }),
  getters: {
    isLogin: (state) => !!state.token,
    role: (state) => state.user?.role || ""
  },
  actions: {
    setLogin(payload) {
      this.token = payload.token;
      this.user = payload;
      localStorage.setItem("token", payload.token);
      localStorage.setItem("user", JSON.stringify(payload));
    },
    logout() {
      this.token = "";
      this.user = null;
      localStorage.removeItem("token");
      localStorage.removeItem("user");
    }
  }
});
