import axios from "axios";
import keycloak from "./services/keycloak";

const httpClient = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || "http://localhost:8080",
  headers: { "Content-Type": "application/json" },
});

httpClient.interceptors.request.use(
  async (config) => {
    if (keycloak.authenticated) {
      await keycloak.updateToken(30);
      config.headers.Authorization = `Bearer ${keycloak.token}`;
    }
    return config;
  },
  (error) => Promise.reject(error),
);

export default httpClient;
