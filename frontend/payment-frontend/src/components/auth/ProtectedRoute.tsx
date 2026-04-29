import { ReactNode } from "react";
import { useCurrentUser } from "../../hooks/usePayments";

interface Props { children: ReactNode; }

export function ProtectedRoute({ children }: Props) {
  const { isAuthenticated, isLoading, isError, login } = useCurrentUser();

  if (isLoading) {
    return (
      <div className="auth-loading">
        <div className="auth-spinner" />
        <span>Verifying session…</span>
      </div>
    );
  }

  if (isError || !isAuthenticated) {
    const returnTo = window.location.pathname + window.location.search;
    login(returnTo);
    return (
      <div className="auth-loading">
        <div className="auth-spinner" />
        <span>Redirecting to login…</span>
      </div>
    );
  }

  return <>{children}</>;
}
