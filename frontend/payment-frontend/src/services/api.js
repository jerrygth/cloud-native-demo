import axios from "axios";
// ═══════════════════════════════════════════════════════════════════
// API SERVICE — Razorpay + BFF Edition
// ═══════════════════════════════════════════════════════════════════
// withCredentials: true  → sends session cookie (BFF pattern)
// CSRF interceptor       → sends X-XSRF-TOKEN header on mutations
// No Authorization header → gateway handles token relay
// ═══════════════════════════════════════════════════════════════════
const GATEWAY_URL = import.meta.env.VITE_API_GATEWAY_URL || "http://localhost:8081";
const apiClient = axios.create({
    baseURL: GATEWAY_URL,
    timeout: 15000,
    headers: { "Content-Type": "application/json" },
    withCredentials: true, // BFF: send session cookie
});
// CSRF token interceptor (BFF cookie-session protection)
function getCsrfToken() {
    const match = document.cookie.split(";").map(c => c.trim())
        .find(c => c.startsWith("XSRF-TOKEN="));
    return match ? decodeURIComponent(match.split("=")[1]) : null;
}
apiClient.interceptors.request.use((config) => {
    if (["post", "put", "delete", "patch"].includes(config.method ?? "")) {
        const token = getCsrfToken();
        if (token)
            config.headers["X-XSRF-TOKEN"] = token;
    }
    return config;
});
apiClient.interceptors.response.use((response) => response, (error) => {
    const apiError = error.response?.data ?? {
        error: "network_error",
        message: error.message || "Network request failed",
        status: error.response?.status ?? 0,
    };
    if (error.response?.status === 401) {
        const isMeEndpoint = (error.config?.url ?? "").includes("/api/me");
        if (!isMeEndpoint) {
            const loginUrl = import.meta.env.VITE_LOGIN_URL ||
                "http://localhost:8081/oauth2/authorization/auth0";
            window.location.href = `${loginUrl}?returnTo=${encodeURIComponent(window.location.pathname)}`;
        }
    }
    return Promise.reject(apiError);
});
// ═══════════════════════════════════════════════════════════════════
// PAYMENT API METHODS — Razorpay 2-step flow
// ═══════════════════════════════════════════════════════════════════
export const paymentApi = {
    // ── GET PUBLIC KEY ──────────────────────────────────────────────
    // Fetch Razorpay key_id to initialise the popup.
    // Called once when the checkout form mounts.
    getPublicKey: async () => {
        const { data } = await apiClient.get("/api/payments/key");
        return data.keyId;
    },
    // ── STEP 1: CREATE ORDER ────────────────────────────────────────
    // Backend creates a Razorpay order and returns the orderId.
    // React uses this orderId to initialise the Razorpay popup.
    createOrder: async (request) => {
        const { data } = await apiClient.post("/api/payments/order", request);
        return data;
    },
    // ── STEP 2: VERIFY PAYMENT ──────────────────────────────────────
    // After Razorpay popup fires success handler, send the 3 IDs to backend.
    // Backend verifies HMAC signature and marks payment as captured.
    verifyPayment: async (request) => {
        const { data } = await apiClient.post("/api/payments/verify", request);
        return data;
    },
    // ── HISTORY ─────────────────────────────────────────────────────
    getHistory: async (page = 0, size = 20) => {
        const { data } = await apiClient.get("/api/payments", {
            params: { page, size },
        });
        return data;
    },
    // ── REFUND ──────────────────────────────────────────────────────
    refund: async (request) => {
        const { data } = await apiClient.post("/api/payments/refund", request);
        return data;
    },
};
export default apiClient;
