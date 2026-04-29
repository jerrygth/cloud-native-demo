import { Link } from "react-router-dom";
import { CheckoutForm } from "../components/payment/CheckoutForm";
import { PaymentHistory } from "../components/payment/PaymentHistory";
import { useEffect } from "react";
import { useUIStore } from "../store/uiStore";
import { useCurrentUser } from "../hooks/useCurrentUser";

// ── DASHBOARD ─────────────────────────────────────────────────────
export function DashboardPage() {
  const { user } = useCurrentUser();
  const hour = new Date().getHours();
  const greeting = hour < 12 ? "Good morning" : hour < 18 ? "Good afternoon" : "Good evening";

  const givenName = user?.firstName || user?.name?.split(" ")[0] || "";

  return (
    <div className="page page--dashboard">
      <div className="page-header">
        <div>
          <h1>{greeting}{givenName ? `, ${givenName}` : ""}.</h1>
          <p className="page-subtitle">Manage your payments and transactions</p>
        </div>
      </div>
      <div className="dashboard-grid">
        <section className="card card--checkout">
          <CheckoutForm />
        </section>
        <section className="card card--info">
          <div className="info-panel">
            <h3>How it works</h3>
            <ol className="guide-steps">
              <li>Enter product details and amount in ₹</li>
              <li>Click <strong>Pay with Razorpay</strong></li>
              <li>Choose UPI, Card, Netbanking or Wallet</li>
              <li>Payment confirmed instantly — no redirect!</li>
            </ol>
            <div className="info-divider" />
            <div className="tech-stack">
              <h4>Supported Methods</h4>
              <div className="tech-tags">
                <span className="tech-tag">🔵 UPI</span>
                <span className="tech-tag">💳 Cards</span>
                <span className="tech-tag">🏦 Netbanking</span>
                <span className="tech-tag">👛 Wallets</span>
                <span className="tech-tag">📅 EMI</span>
              </div>
            </div>
          </div>
        </section>
      </div>
    </div>
  );
}

// ── HISTORY ───────────────────────────────────────────────────────
export function HistoryPage() {
  return (
    <div className="page">
      <div className="page-header">
        <div>
          <h1>Transactions</h1>
          <p className="page-subtitle">All your payment history in one place</p>
        </div>
      </div>
      <PaymentHistory />
    </div>
  );
}

// ── PAYMENT SUCCESS ───────────────────────────────────────────────
export function PaymentSuccessPage() {
  const setRedirecting = useUIStore((s) => s.setRedirecting);
  useEffect(() => { setRedirecting(false); }, [setRedirecting]);

  return (
    <div className="page page--centered">
      <div className="result-card result-card--success">
        <div className="result-card__icon">✓</div>
        <h1>Payment successful!</h1>
        <p>Your payment has been captured by Razorpay.</p>
        <div className="result-card__actions">
          <Link to="/history" className="btn btn--primary">View History</Link>
          <Link to="/dashboard" className="btn btn--ghost">New Payment</Link>
        </div>
      </div>
    </div>
  );
}

// ── PAYMENT FAILED ────────────────────────────────────────────────
export function PaymentFailedPage() {
  const setRedirecting = useUIStore((s) => s.setRedirecting);
  useEffect(() => { setRedirecting(false); }, [setRedirecting]);

  return (
    <div className="page page--centered">
      <div className="result-card result-card--neutral">
        <div className="result-card__icon">◌</div>
        <h1>Payment not completed</h1>
        <p>No charge was made. You can try again whenever you're ready.</p>
        <div className="result-card__actions">
          <Link to="/dashboard" className="btn btn--primary">Try Again</Link>
        </div>
      </div>
    </div>
  );
}

// ── LOGIN ─────────────────────────────────────────────────────────
export function LoginPage() {
  const { isAuthenticated, isLoading, login } = useCurrentUser();

  useEffect(() => {
    if (!isLoading && isAuthenticated) window.location.replace("/dashboard");
  }, [isAuthenticated, isLoading]);

  return (
    <div className="login-page">
      <div className="login-card">
        <div className="login-card__brand">
          <span className="brand-logo brand-logo--lg">₹</span>
          <h1>PayFlow India</h1>
          <p>Quarkus + Razorpay payment demo</p>
        </div>
        <div className="login-card__features">
          <div className="feature-item"><span>🔵</span><span>UPI, Cards, Netbanking &amp; Wallets</span></div>
          <div className="feature-item"><span>🔒</span><span>BFF Pattern — secure session auth</span></div>
          <div className="feature-item"><span>⚡</span><span>Instant confirmation — no redirect</span></div>
        </div>
        <button
          className="btn btn--primary btn--lg btn--full"
          onClick={() => login("/dashboard")}
          disabled={isLoading}
        >
          {isLoading ? <><span className="spinner spinner--sm" /> Checking…</> : "Sign in with Auth0 →"}
        </button>
        <p className="login-card__note">Demo · Test mode · No real transactions</p>
      </div>
    </div>
  );
}

// ── 404 ───────────────────────────────────────────────────────────
export function NotFoundPage() {
  return (
    <div className="page page--centered">
      <div className="result-card">
        <div className="result-card__icon result-card__icon--large">404</div>
        <h1>Page not found</h1>
        <Link to="/dashboard" className="btn btn--primary">Back to dashboard</Link>
      </div>
    </div>
  );
}
