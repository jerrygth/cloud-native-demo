import { jsx as _jsx, jsxs as _jsxs, Fragment as _Fragment } from "react/jsx-runtime";
import { useState } from "react";
import { format } from "date-fns";
import { usePaymentHistory, useRefundMutation } from "../../hooks/usePayments";
// Status display config — Razorpay statuses (CAPTURED replaces COMPLETED)
const STATUS_CONFIG = {
    CREATED: { label: "Pending", className: "badge--warning" },
    AUTHORIZED: { label: "Authorized", className: "badge--info" },
    CAPTURED: { label: "Captured", className: "badge--success" },
    FAILED: { label: "Failed", className: "badge--error" },
    REFUNDED: { label: "Refunded", className: "badge--neutral" },
    PARTIALLY_REFUNDED: { label: "Part. Refund", className: "badge--neutral" },
};
// Payment method icons for Indian payment methods
const METHOD_ICON = {
    upi: "🔵",
    card: "💳",
    netbanking: "🏦",
    wallet: "👛",
    emi: "📅",
    paylater: "⏳",
    unknown: "💰",
};
function StatusBadge({ status }) {
    const config = STATUS_CONFIG[status] ?? { label: status, className: "badge--neutral" };
    return _jsx("span", { className: `badge ${config.className}`, children: config.label });
}
function MethodBadge({ method }) {
    if (!method)
        return null;
    const icon = METHOD_ICON[method] ?? "💰";
    return (_jsxs("span", { className: "method-badge", children: [icon, " ", method.toUpperCase()] }));
}
// ── REFUND MODAL ──────────────────────────────────────────────────
function RefundModal({ payment, onClose }) {
    const { mutate: refund, isPending } = useRefundMutation();
    const [speed, setSpeed] = useState("normal");
    const handleRefund = () => {
        refund({ paymentId: payment.id, speed }, { onSuccess: onClose });
    };
    return (_jsx("div", { className: "modal-backdrop", onClick: (e) => e.target === e.currentTarget && onClose(), children: _jsxs("div", { className: "modal", children: [_jsxs("div", { className: "modal__header", children: [_jsx("h3", { children: "Process Refund" }), _jsx("button", { className: "modal__close", onClick: onClose, children: "\u00D7" })] }), _jsxs("div", { className: "modal__body", children: [_jsxs("div", { className: "refund-summary", children: [_jsxs("div", { className: "refund-summary__row", children: [_jsx("span", { children: "Order" }), _jsx("code", { children: payment.razorpayOrderId })] }), _jsxs("div", { className: "refund-summary__row", children: [_jsx("span", { children: "Amount" }), _jsxs("strong", { children: ["\u20B9", (payment.amount / 100).toFixed(2)] })] }), payment.paymentMethod && (_jsxs("div", { className: "refund-summary__row", children: [_jsx("span", { children: "Paid via" }), _jsx(MethodBadge, { method: payment.paymentMethod })] }))] }), _jsxs("div", { className: "form-group", children: [_jsx("label", { className: "form-label", children: "Refund speed" }), _jsxs("select", { className: "form-input form-select", value: speed, onChange: (e) => setSpeed(e.target.value), children: [_jsx("option", { value: "normal", children: "Normal (5-7 business days)" }), _jsx("option", { value: "optimum", children: "Optimum (instant if eligible)" })] })] })] }), _jsxs("div", { className: "modal__footer", children: [_jsx("button", { className: "btn btn--ghost", onClick: onClose, disabled: isPending, children: "Cancel" }), _jsx("button", { className: `btn btn--danger ${isPending ? "btn--loading" : ""}`, onClick: handleRefund, disabled: isPending, children: isPending ? _jsxs(_Fragment, { children: [_jsx("span", { className: "spinner spinner--sm" }), "Processing..."] }) : "Confirm Refund" })] })] }) }));
}
// ── PAYMENT ROW ───────────────────────────────────────────────────
function PaymentRow({ payment }) {
    const [showRefund, setShowRefund] = useState(false);
    const canRefund = payment.status === "CAPTURED";
    return (_jsxs(_Fragment, { children: [_jsxs("tr", { className: "table__row", children: [_jsx("td", { className: "table__cell", children: _jsx("code", { className: "order-id", children: payment.razorpayOrderId }) }), _jsxs("td", { className: "table__cell table__cell--amount", children: [_jsxs("span", { className: "amount", children: ["\u20B9", (payment.amount / 100).toFixed(2)] }), _jsx("span", { className: "currency", children: payment.currency })] }), _jsx("td", { className: "table__cell", children: _jsx(StatusBadge, { status: payment.status }) }), _jsx("td", { className: "table__cell", children: _jsx(MethodBadge, { method: payment.paymentMethod }) }), _jsx("td", { className: "table__cell table__cell--date", children: format(new Date(payment.createdAt), "dd MMM yyyy, HH:mm") }), _jsx("td", { className: "table__cell table__cell--actions", children: canRefund && (_jsx("button", { className: "btn btn--ghost btn--sm", onClick: () => setShowRefund(true), children: "Refund" })) })] }), showRefund && _jsx(RefundModal, { payment: payment, onClose: () => setShowRefund(false) })] }));
}
// ── MAIN COMPONENT ────────────────────────────────────────────────
export function PaymentHistory() {
    const [page, setPage] = useState(0);
    const { data: payments, isLoading, isError, error, refetch } = usePaymentHistory(page);
    if (isLoading) {
        return (_jsxs("div", { className: "history-container", children: [_jsx("div", { className: "history-header", children: _jsx("h2", { children: "Payment History" }) }), _jsx("div", { className: "table-skeleton", children: Array.from({ length: 5 }).map((_, i) => (_jsx("div", { className: "skeleton-row", style: { animationDelay: `${i * 80}ms` } }, i))) })] }));
    }
    if (isError) {
        return (_jsx("div", { className: "history-container", children: _jsxs("div", { className: "empty-state empty-state--error", children: [_jsx("div", { className: "empty-state__icon", children: "\u26A0" }), _jsx("p", { children: error?.message || "Failed to load payment history" }), _jsx("button", { className: "btn btn--ghost btn--sm", onClick: () => refetch(), children: "Try again" })] }) }));
    }
    const isEmpty = !payments || payments.length === 0;
    return (_jsxs("div", { className: "history-container", children: [_jsxs("div", { className: "history-header", children: [_jsxs("div", { children: [_jsx("h2", { children: "Payment History" }), payments && (_jsxs("p", { className: "history-count", children: [payments.length, " transaction", payments.length !== 1 ? "s" : ""] }))] }), _jsx("button", { className: "btn btn--ghost btn--sm", onClick: () => refetch(), children: "\u21BB Refresh" })] }), isEmpty ? (_jsxs("div", { className: "empty-state", children: [_jsx("div", { className: "empty-state__icon", children: "\uD83D\uDCED" }), _jsx("p", { children: "No payments yet" }), _jsx("span", { children: "Use the form above to make your first payment" })] })) : (_jsxs(_Fragment, { children: [_jsx("div", { className: "table-wrapper", children: _jsxs("table", { className: "table", children: [_jsx("thead", { children: _jsxs("tr", { children: [_jsx("th", { className: "table__th", children: "Order ID" }), _jsx("th", { className: "table__th", children: "Amount" }), _jsx("th", { className: "table__th", children: "Status" }), _jsx("th", { className: "table__th", children: "Method" }), _jsx("th", { className: "table__th", children: "Date" }), _jsx("th", { className: "table__th", children: "Actions" })] }) }), _jsx("tbody", { children: payments.map((p) => _jsx(PaymentRow, { payment: p }, p.id)) })] }) }), _jsxs("div", { className: "pagination", children: [_jsx("button", { className: "btn btn--ghost btn--sm", onClick: () => setPage(p => Math.max(0, p - 1)), disabled: page === 0, children: "\u2190 Prev" }), _jsxs("span", { className: "pagination__page", children: ["Page ", page + 1] }), _jsx("button", { className: "btn btn--ghost btn--sm", onClick: () => setPage(p => p + 1), disabled: !payments || payments.length < 20, children: "Next \u2192" })] })] }))] }));
}
