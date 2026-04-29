import { Link, NavLink, Outlet } from "react-router-dom";
import { useUIStore } from "../../store/uiStore";
import { useCurrentUser, useUpdatePreferences } from "../../hooks/useCurrentUser";

export function AppLayout() {
  const { user, logout } = useCurrentUser();
  const { theme, toggleTheme } = useUIStore();
  const { mutate: updatePreferences } = useUpdatePreferences();

  const handleThemeToggle = () => {
    const newTheme = theme === "dark" ? "light" : "dark";
    toggleTheme();                          // instant Zustand update
    updatePreferences({ theme: newTheme }); // persist to server in background
  };

  return (
    <div className="app-shell" data-theme={theme}>
      <header className="navbar">
        <div className="navbar__brand">
          <Link to="/dashboard" className="brand-link">
            <span className="brand-logo">₹</span>
            <span className="brand-name">PayFlow</span>
          </Link>
          <span className="brand-tag">razorpay</span>
        </div>

        <nav className="navbar__nav">
          <NavLink to="/dashboard" className={({ isActive }) =>
            `nav-link ${isActive ? "nav-link--active" : ""}`}>
            Dashboard
          </NavLink>
          <NavLink to="/history" className={({ isActive }) =>
            `nav-link ${isActive ? "nav-link--active" : ""}`}>
            History
          </NavLink>
        </nav>

        <div className="navbar__actions">
          <button
            className="icon-btn"
            onClick={handleThemeToggle}
            aria-label="Toggle theme"
            title={`Switch to ${theme === "dark" ? "light" : "dark"} mode`}
          >
            {theme === "dark" ? "☀" : "◑"}
          </button>

          {user && (
            <div className="user-menu">
              {user.picture
                ? <img src={user.picture} alt={user.name} className="user-avatar" />
                : <div className="user-avatar user-avatar--fallback">
                    {user.name?.[0]?.toUpperCase() ?? "U"}
                  </div>
              }
              <div className="user-info">
                <span className="user-name">{user.name}</span>
                <span className="user-email">{user.email}</span>
              </div>
              <button className="btn btn--ghost btn--sm" onClick={logout}>
                Logout
              </button>
            </div>
          )}
        </div>
      </header>

      <main className="main-content">
        <Outlet />
      </main>

      <footer className="app-footer">
        <span>PayFlow · Quarkus + Razorpay + Auth0 BFF</span>
        {import.meta.env.MODE === "development" && (
          <span className="badge badge--warning">DEV</span>
        )}
      </footer>
    </div>
  );
}