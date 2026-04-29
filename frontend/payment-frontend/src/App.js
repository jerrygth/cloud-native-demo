import { jsx as _jsx, jsxs as _jsxs } from "react/jsx-runtime";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { ReactQueryDevtools } from "@tanstack/react-query-devtools";
import { BrowserRouter, Navigate, Route, Routes } from "react-router-dom";
import { Toaster } from "react-hot-toast";
import { ProtectedRoute } from "./components/auth/ProtectedRoute";
import { ErrorBoundary } from "./components/common/ErrorBoundary";
import { AppLayout } from "./components/layout/AppLayout";
import { DashboardPage, HistoryPage, LoginPage, NotFoundPage, PaymentSuccessPage, PaymentFailedPage, } from "./pages";
const queryClient = new QueryClient({
    defaultOptions: {
        queries: {
            staleTime: 1000 * 60 * 5,
            retry: (count, error) => {
                if (error?.status >= 400 && error?.status < 500)
                    return false;
                return count < 2;
            },
            refetchOnWindowFocus: true,
        },
        mutations: { retry: false },
    },
});
function AppRoutes() {
    return (_jsxs(Routes, { children: [_jsx(Route, { path: "/login", element: _jsx(LoginPage, {}) }), _jsx(Route, { path: "/payment/success", element: _jsx(PaymentSuccessPage, {}) }), _jsx(Route, { path: "/payment/failed", element: _jsx(PaymentFailedPage, {}) }), _jsxs(Route, { element: _jsx(ProtectedRoute, { children: _jsx(AppLayout, {}) }), children: [_jsx(Route, { path: "/dashboard", element: _jsx(DashboardPage, {}) }), _jsx(Route, { path: "/history", element: _jsx(ErrorBoundary, { children: _jsx(HistoryPage, {}) }) })] }), _jsx(Route, { path: "/", element: _jsx(Navigate, { to: "/dashboard", replace: true }) }), _jsx(Route, { path: "*", element: _jsx(NotFoundPage, {}) })] }));
}
export default function App() {
    return (_jsx(BrowserRouter, { children: _jsxs(QueryClientProvider, { client: queryClient, children: [_jsx(ErrorBoundary, { children: _jsx(AppRoutes, {}) }), _jsx(Toaster, { position: "top-right", toastOptions: {
                        duration: 4000,
                        style: {
                            background: "var(--surface-2)",
                            color: "var(--text-primary)",
                            border: "1px solid var(--border)",
                            borderRadius: "8px",
                            fontSize: "14px",
                        },
                        success: { iconTheme: { primary: "var(--success)", secondary: "var(--bg)" } },
                        error: { iconTheme: { primary: "var(--error)", secondary: "var(--bg)" } },
                    } }), import.meta.env.DEV && _jsx(ReactQueryDevtools, { initialIsOpen: false })] }) }));
}
