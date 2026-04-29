import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { ReactQueryDevtools } from "@tanstack/react-query-devtools";
import { BrowserRouter, Navigate, Route, Routes } from "react-router-dom";
import { Toaster } from "react-hot-toast";
import { ProtectedRoute } from "./components/auth/ProtectedRoute";
import { ErrorBoundary } from "./components/common/ErrorBoundary";
import { AppLayout } from "./components/layout/AppLayout";
import {
  DashboardPage, HistoryPage, LoginPage,
  NotFoundPage, PaymentSuccessPage, PaymentFailedPage,
} from "./pages";

const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      staleTime: 1000 * 60 * 5,
      retry: (count, error: any) => {
        if (error?.status >= 400 && error?.status < 500) return false;
        return count < 2;
      },
      refetchOnWindowFocus: true,
    },
    mutations: { retry: false },
  },
});

function AppRoutes() {
  return (
    <Routes>
      <Route path="/login" element={<LoginPage />} />
      <Route path="/payment/success" element={<PaymentSuccessPage />} />
      <Route path="/payment/failed" element={<PaymentFailedPage />} />

      <Route element={<ProtectedRoute><AppLayout /></ProtectedRoute>}>
        <Route path="/dashboard" element={<DashboardPage />} />
        <Route path="/history" element={<ErrorBoundary><HistoryPage /></ErrorBoundary>} />
      </Route>

      <Route path="/" element={<Navigate to="/dashboard" replace />} />
      <Route path="*" element={<NotFoundPage />} />
    </Routes>
  );
}

export default function App() {
  return (
    <BrowserRouter>
      <QueryClientProvider client={queryClient}>
        <ErrorBoundary>
          <AppRoutes />
        </ErrorBoundary>
        <Toaster
          position="top-right"
          toastOptions={{
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
          }}
        />
        {import.meta.env.DEV && <ReactQueryDevtools initialIsOpen={false} />}
      </QueryClientProvider>
    </BrowserRouter>
  );
}
