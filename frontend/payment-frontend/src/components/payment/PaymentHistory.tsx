import { useState } from "react";
import { format } from "date-fns";
import { usePaymentHistory, useRefundMutation } from "../../hooks/usePayments";
import { Payment, PaymentStatus } from "../../types";

const STATUS_CONFIG: Record<PaymentStatus, { label: string; className: string }> = {
  CREATED:            { label: "Pending",    className: "badge--warning" },
  AUTHORIZED:         { label: "Authorized", className: "badge--info" },
  CAPTURED:           { label: "Captured",   className: "badge--success" },
  FAILED:             { label: "Failed",     className: "badge--error" },
  REFUNDED:           { label: "Refunded",   className: "badge--neutral" },
  PARTIALLY_REFUNDED: { label: "Part. Refund", className: "badge--neutral" },
};

// Payment method icons for Indian payment methods
const METHOD_ICON: Record<string, string> = {
  upi:        "🔵",
  card:       "💳",
  netbanking: "🏦",
  wallet:     "👛",
  emi:        "📅",
  paylater:   "⏳"
};

function StatusBadge({ status }: { status: PaymentStatus }) {
  const config = STATUS_CONFIG[status] ?? { label: status, className: "badge--neutral" };
  return <span className={`badge ${config.className}`}>{config.label}</span>;
}

function MethodBadge({ method }: { method: string | null }) {
  if (!method) return null;
  const icon = METHOD_ICON[method] ?? "💰";
  return (
    <span className="method-badge">
      {icon} {method.toUpperCase()}
    </span>
  );
}

// ── REFUND MODAL ──────────────────────────────────────────────────
function RefundModal({ payment, onClose }: { payment: Payment; onClose: () => void }) {
  const { mutate: refund, isPending } = useRefundMutation();
  const [speed, setSpeed] = useState<"normal" | "optimum">("normal");

  const handleRefund = () => {
    refund({ paymentId: payment.id, speed }, { onSuccess: onClose });
  };

  return (
    <div className="modal-backdrop" onClick={(e) => e.target === e.currentTarget && onClose()}>
      <div className="modal">
        <div className="modal__header">
          <h3>Process Refund</h3>
          <button className="modal__close" onClick={onClose}>×</button>
        </div>
        <div className="modal__body">
          <div className="refund-summary">
            <div className="refund-summary__row">
              <span>Order</span>
              <code>{payment.razorpayOrderId}</code>
            </div>
            <div className="refund-summary__row">
              <span>Amount</span>
              {/* Razorpay amounts are in paise — convert to rupees */}
              <strong>₹{(payment.amount / 100).toFixed(2)}</strong>
            </div>
            {payment.paymentMethod && (
              <div className="refund-summary__row">
                <span>Paid via</span>
                <MethodBadge method={payment.paymentMethod} />
              </div>
            )}
          </div>
          <div className="form-group">
            <label className="form-label">Refund speed</label>
            <select
              className="form-input form-select"
              value={speed}
              onChange={(e) => setSpeed(e.target.value as typeof speed)}
            >
              <option value="normal">Normal (5-7 business days)</option>
              <option value="optimum">Optimum (instant if eligible)</option>
            </select>
          </div>
        </div>
        <div className="modal__footer">
          <button className="btn btn--ghost" onClick={onClose} disabled={isPending}>Cancel</button>
          <button
            className={`btn btn--danger ${isPending ? "btn--loading" : ""}`}
            onClick={handleRefund}
            disabled={isPending}
          >
            {isPending ? <><span className="spinner spinner--sm" />Processing...</> : "Confirm Refund"}
          </button>
        </div>
      </div>
    </div>
  );
}

// ── PAYMENT ROW ───────────────────────────────────────────────────
function PaymentRow({ payment }: { payment: Payment }) {
  const [showRefund, setShowRefund] = useState(false);
  const canRefund = payment.status === "CAPTURED";

  return (
    <>
      <tr className="table__row">
        <td className="table__cell">
          <code className="order-id">{payment.razorpayOrderId}</code>
        </td>
        <td className="table__cell table__cell--amount">
          {/* Display in rupees (divide by 100) with ₹ symbol */}
          <span className="amount">₹{(payment.amount / 100).toFixed(2)}</span>
          <span className="currency">{payment.currency}</span>
        </td>
        <td className="table__cell">
          <StatusBadge status={payment.status} />
        </td>
        <td className="table__cell">
          <MethodBadge method={payment.paymentMethod} />
        </td>
        <td className="table__cell table__cell--date">
          {format(new Date(payment.createdAt), "dd MMM yyyy, HH:mm")}
        </td>
        <td className="table__cell table__cell--actions">
          {canRefund && (
            <button className="btn btn--ghost btn--sm" onClick={() => setShowRefund(true)}>
              Refund
            </button>
          )}
        </td>
      </tr>
      {showRefund && <RefundModal payment={payment} onClose={() => setShowRefund(false)} />}
    </>
  );
}

// ── MAIN COMPONENT ────────────────────────────────────────────────
export function PaymentHistory() {
  const [page, setPage] = useState(0);
  const { data: payments, isLoading, isError, error, refetch } = usePaymentHistory(page);

  if (isLoading) {
    return (
      <div className="history-container">
        <div className="history-header"><h2>Payment History</h2></div>
        <div className="table-skeleton">
          {Array.from({ length: 5 }).map((_, i) => (
            <div key={i} className="skeleton-row" style={{ animationDelay: `${i * 80}ms` }} />
          ))}
        </div>
      </div>
    );
  }

  if (isError) {
    return (
      <div className="history-container">
        <div className="empty-state empty-state--error">
          <div className="empty-state__icon">⚠</div>
          <p>{(error as any)?.message || "Failed to load payment history"}</p>
          <button className="btn btn--ghost btn--sm" onClick={() => refetch()}>Try again</button>
        </div>
      </div>
    );
  }

  const isEmpty = !payments || payments.length === 0;

  return (
    <div className="history-container">
      <div className="history-header">
        <div>
          <h2>Payment History</h2>
          {payments && (
            <p className="history-count">
              {payments.length} transaction{payments.length !== 1 ? "s" : ""}
            </p>
          )}
        </div>
        <button className="btn btn--ghost btn--sm" onClick={() => refetch()}>↻ Refresh</button>
      </div>

      {isEmpty ? (
        <div className="empty-state">
          <div className="empty-state__icon">📭</div>
          <p>No payments yet</p>
          <span>Use the form above to make your first payment</span>
        </div>
      ) : (
        <>
          <div className="table-wrapper">
            <table className="table">
              <thead>
                <tr>
                  <th className="table__th">Order ID</th>
                  <th className="table__th">Amount</th>
                  <th className="table__th">Status</th>
                  <th className="table__th">Method</th>
                  <th className="table__th">Date</th>
                  <th className="table__th">Actions</th>
                </tr>
              </thead>
              <tbody>
                {payments.map((p) => <PaymentRow key={p.id} payment={p} />)}
              </tbody>
            </table>
          </div>
          <div className="pagination">
            <button className="btn btn--ghost btn--sm" onClick={() => setPage(p => Math.max(0, p - 1))} disabled={page === 0}>← Prev</button>
            <span className="pagination__page">Page {page + 1}</span>
            <button className="btn btn--ghost btn--sm" onClick={() => setPage(p => p + 1)} disabled={!payments || payments.length < 20}>Next →</button>
          </div>
        </>
      )}
    </div>
  );
}
