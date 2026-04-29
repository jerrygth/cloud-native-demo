import axios, { AxiosError, AxiosInstance } from "axios";
import { ApiError, OrderRequest, OrderResponse, Payment, RefundRequest, RefundResponse, VerifyRequest, VerifyResponse } from "../types";

function getEnv(key: string): string {
  return (window as any).__ENV__?.[key]
    ?? import.meta.env[key]
    ?? '';
}

const GATEWAY_URL = getEnv('VITE_API_GATEWAY_URL') || 'http://localhost:8081';

const apiClient: AxiosInstance = axios.create({
  baseURL: GATEWAY_URL,
  timeout: 15_000,
  headers: { "Content-Type": "application/json" },
  withCredentials: true,
});

// CSRF token interceptor (BFF cookie-session protection)
function getCsrfToken(): string | null {
  const match = document.cookie.split(";").map(c => c.trim())
    .find(c => c.startsWith("XSRF-TOKEN="));
  return match ? decodeURIComponent(match.split("=")[1]) : null;
}

apiClient.interceptors.request.use((config) => {
  if (["post", "put", "delete", "patch"].includes(config.method ?? "")) {
    const token = getCsrfToken();
    if (token) config.headers["X-XSRF-TOKEN"] = token;
  }
  return config;
});

apiClient.interceptors.response.use(
  (response) => response,
  (error: AxiosError<ApiError>) => {
    const apiError: ApiError = error.response?.data ?? {
      error: "network_error",
      message: error.message || "Network request failed",
      status: error.response?.status ?? 0,
    };
    if (error.response?.status === 401) {
      const isMeEndpoint = (error.config?.url ?? "").includes("/api/me");
      if (!isMeEndpoint) {
        const loginUrl = getEnv(VITE_LOGIN_URL) ||
          "http://localhost:8081/oauth2/authorization/auth0";
        window.location.href = `${loginUrl}?returnTo=${encodeURIComponent(window.location.pathname)}`;
      }
    }
    return Promise.reject(apiError);
  }
);


export const paymentApi = {


  getPublicKey: async (): Promise<string> => {
    const { data } = await apiClient.get<{ keyId: string }>("/api/payments/key");
    return data.keyId;
  },


  createOrder: async (request: OrderRequest): Promise<OrderResponse> => {
    const { data } = await apiClient.post<OrderResponse>("/api/payments/order", request);
    return data;
  },

  verifyPayment: async (request: VerifyRequest): Promise<VerifyResponse> => {
    const { data } = await apiClient.post<VerifyResponse>("/api/payments/verify", request);
    return data;
  },

  // ── HISTORY ─────────────────────────────────────────────────────
  getHistory: async (page = 0, size = 20): Promise<Payment[]> => {
    const { data } = await apiClient.get<Payment[]>("/api/payments", {
      params: { page, size },
    });
    return data;
  },

  // ── REFUND ──────────────────────────────────────────────────────
  refund: async (request: RefundRequest): Promise<RefundResponse> => {
    const { data } = await apiClient.post<RefundResponse>("/api/payments/refund", request);
    return data;
  },
};

export default apiClient;
