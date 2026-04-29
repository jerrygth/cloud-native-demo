import { jsx as _jsx, jsxs as _jsxs, Fragment as _Fragment } from "react/jsx-runtime";
import { useCurrentUser } from "../../hooks/usePayments";
export function ProtectedRoute({ children }) {
    const { isAuthenticated, isLoading, isError, login } = useCurrentUser();
    if (isLoading) {
        return (_jsxs("div", { className: "auth-loading", children: [_jsx("div", { className: "auth-spinner" }), _jsx("span", { children: "Verifying session\u2026" })] }));
    }
    if (isError || !isAuthenticated) {
        const returnTo = window.location.pathname + window.location.search;
        login(returnTo);
        return (_jsxs("div", { className: "auth-loading", children: [_jsx("div", { className: "auth-spinner" }), _jsx("span", { children: "Redirecting to login\u2026" })] }));
    }
    return _jsx(_Fragment, { children: children });
}
