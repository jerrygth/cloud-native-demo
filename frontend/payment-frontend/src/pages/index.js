import { jsxs as _jsxs, jsx as _jsx, Fragment as _Fragment } from "react/jsx-runtime";
import { Link } from "react-router-dom";
import { CheckoutForm } from "../components/payment/CheckoutForm";
import { PaymentHistory } from "../components/payment/PaymentHistory";
import { useEffect } from "react";
import { useUIStore } from "../store/uiStore";
import { useCurrentUser } from "../hooks/usePayments";
// ── DASHBOARD ─────────────────────────────────────────────────────
export function DashboardPage() {
    const { user } = useCurrentUser();
    const hour = new Date().getHours();
    const greeting = hour < 12 ? "Good morning" : hour < 18 ? "Good afternoon" : "Good evening";
    return (_jsxs("div", { className: "page page--dashboard", children: [_jsx("div", { className: "page-header", children: _jsxs("div", { children: [_jsxs("h1", { children: [greeting, user?.name ? `, ${user.name.split(" ")[0]}` : "", "."] }), _jsx("p", { className: "page-subtitle", children: "Manage your payments and transactions" })] }) }), _jsxs("div", { className: "dashboard-grid", children: [_jsx("section", { className: "card card--checkout", children: _jsx(CheckoutForm, {}) }), _jsx("section", { className: "card card--info", children: _jsxs("div", { className: "info-panel", children: [_jsx("h3", { children: "How it works" }), _jsxs("ol", { className: "guide-steps", children: [_jsx("li", { children: "Enter product details and amount in \u20B9" }), _jsxs("li", { children: ["Click ", _jsx("strong", { children: "Pay with Razorpay" })] }), _jsx("li", { children: "Choose UPI, Card, Netbanking or Wallet" }), _jsx("li", { children: "Payment confirmed instantly \u2014 no redirect!" })] }), _jsx("div", { className: "info-divider" }), _jsxs("div", { className: "tech-stack", children: [_jsx("h4", { children: "Supported Methods" }), _jsxs("div", { className: "tech-tags", children: [_jsx("span", { className: "tech-tag", children: "\uD83D\uDD35 UPI" }), _jsx("span", { className: "tech-tag", children: "\uD83D\uDCB3 Debit/Credit Card" }), _jsx("span", { className: "tech-tag", children: "\uD83C\uDFE6 Netbanking" }), _jsx("span", { className: "tech-tag", children: "\uD83D\uDC5B Paytm / PhonePe" }), _jsx("span", { className: "tech-tag", children: "\uD83D\uDCC5 EMI" }), _jsx("span", { className: "tech-tag", children: "\u23F3 Pay Later" })] })] }), _jsx("div", { className: "info-divider" }), _jsxs("div", { className: "tech-stack", children: [_jsx("h4", { children: "Tech Stack" }), _jsxs("div", { className: "tech-tags", children: [_jsx("span", { className: "tech-tag", children: "Razorpay" }), _jsx("span", { className: "tech-tag", children: "Quarkus" }), _jsx("span", { className: "tech-tag", children: "React 18" }), _jsx("span", { className: "tech-tag", children: "BFF Pattern" }), _jsx("span", { className: "tech-tag", children: "Auth0" })] })] })] }) })] })] }));
}
// ── HISTORY ───────────────────────────────────────────────────────
export function HistoryPage() {
    return (_jsxs("div", { className: "page", children: [_jsx("div", { className: "page-header", children: _jsxs("div", { children: [_jsx("h1", { children: "Transactions" }), _jsx("p", { className: "page-subtitle", children: "All your payment history in one place" })] }) }), _jsx(PaymentHistory, {})] }));
}
// ── PAYMENT SUCCESS ───────────────────────────────────────────────
// NOTE: With Razorpay, the popup stays on YOUR page after payment —
// there is no redirect. This page exists for cases where Razorpay
// still redirects (e.g. UPI deep links on mobile) or for future use.
export function PaymentSuccessPage() {
    const setRedirecting = useUIStore((s) => s.setRedirecting);
    useEffect(() => { setRedirecting(false); }, [setRedirecting]);
    return (_jsx("div", { className: "page page--centered", children: _jsxs("div", { className: "result-card result-card--success", children: [_jsx("div", { className: "result-card__icon", children: "\u2713" }), _jsx("h1", { children: "Payment successful!" }), _jsx("p", { children: "Your payment has been captured by Razorpay." }), _jsxs("div", { className: "result-card__actions", children: [_jsx(Link, { to: "/history", className: "btn btn--primary", children: "View History" }), _jsx(Link, { to: "/dashboard", className: "btn btn--ghost", children: "New Payment" })] })] }) }));
}
// ── PAYMENT FAILED ────────────────────────────────────────────────
export function PaymentFailedPage() {
    const setRedirecting = useUIStore((s) => s.setRedirecting);
    useEffect(() => { setRedirecting(false); }, [setRedirecting]);
    return (_jsx("div", { className: "page page--centered", children: _jsxs("div", { className: "result-card result-card--neutral", children: [_jsx("div", { className: "result-card__icon", children: "\u25CC" }), _jsx("h1", { children: "Payment not completed" }), _jsx("p", { children: "No charge was made. You can try again whenever you're ready." }), _jsx("div", { className: "result-card__actions", children: _jsx(Link, { to: "/dashboard", className: "btn btn--primary", children: "Try Again" }) })] }) }));
}
// ── LOGIN ─────────────────────────────────────────────────────────
export function LoginPage() {
    const { isAuthenticated, isLoading, login } = useCurrentUser();
    useEffect(() => {
        if (!isLoading && isAuthenticated)
            window.location.replace("/dashboard");
    }, [isAuthenticated, isLoading]);
    return (_jsx("div", { className: "login-page", children: _jsxs("div", { className: "login-card", children: [_jsxs("div", { className: "login-card__brand", children: [_jsx("span", { className: "brand-logo brand-logo--lg", children: "\u20B9" }), _jsx("h1", { children: "PayFlow India" }), _jsx("p", { children: "Quarkus + Razorpay payment demo" })] }), _jsxs("div", { className: "login-card__features", children: [_jsxs("div", { className: "feature-item", children: [_jsx("span", { children: "\uD83D\uDD35" }), _jsx("span", { children: "UPI, Cards, Netbanking & Wallets" })] }), _jsxs("div", { className: "feature-item", children: [_jsx("span", { children: "\uD83D\uDD12" }), _jsx("span", { children: "BFF Pattern \u2014 secure session auth" })] }), _jsxs("div", { className: "feature-item", children: [_jsx("span", { children: "\u26A1" }), _jsx("span", { children: "Instant payment confirmation \u2014 no redirect" })] })] }), _jsx("button", { className: "btn btn--primary btn--lg btn--full", onClick: () => login("/dashboard"), disabled: isLoading, children: isLoading ? _jsxs(_Fragment, { children: [_jsx("span", { className: "spinner spinner--sm" }), " Checking\u2026"] }) : "Sign in with Auth0 →" }), _jsx("p", { className: "login-card__note", children: "Demo \u00B7 Test mode \u00B7 No real transactions" })] }) }));
}
// ── 404 ───────────────────────────────────────────────────────────
export function NotFoundPage() {
    return (_jsx("div", { className: "page page--centered", children: _jsxs("div", { className: "result-card", children: [_jsx("div", { className: "result-card__icon result-card__icon--large", children: "404" }), _jsx("h1", { children: "Page not found" }), _jsx(Link, { to: "/dashboard", className: "btn btn--primary", children: "Back to dashboard" })] }) }));
}
