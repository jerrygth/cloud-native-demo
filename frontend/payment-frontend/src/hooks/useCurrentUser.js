import { useQuery, useQueryClient } from "@tanstack/react-query";
import apiClient from "../services/api";
// ═══════════════════════════════════════════════════════════════════
// useCurrentUser — BFF SESSION HOOK
// ═══════════════════════════════════════════════════════════════════
//
// BFF PATTERN — WHY THIS EXISTS:
//   In the PKCE flow, Auth0 SDK's useAuth0() told us who the user is
//   by reading the JWT stored in the browser.
//
//   In the BFF pattern, the browser has NO token — only an HttpOnly
//   session cookie. JavaScript cannot read it. So we ask the GATEWAY
//   "who am I?" via a /api/me endpoint. The gateway reads its own
//   server-side session, finds the Auth0 token stored there, and
//   returns the user's profile.
//
// WHAT /api/me RETURNS:
//   The gateway reads the authenticated principal from its OAuth2
//   session and returns the user profile from the OIDC id_token.
//   See: API Gateway → UserInfoController.java
//
// REACT QUERY BEHAVIOUR:
//   - Cached for the session lifetime (staleTime: Infinity)
//   - Re-fetched on window focus (catches session expiry)
//   - On 401 → user is not logged in (cookie expired or invalid)
//   - retry: false → don't hammer the server if session is gone
//
// USAGE (replaces useAuth0() everywhere):
//   const { user, logout, isLoading } = useCurrentUser();
// ═══════════════════════════════════════════════════════════════════
const LOGIN_URL = import.meta.env.VITE_LOGIN_URL ||
    "http://localhost:8081/oauth2/authorization/auth0";
const LOGOUT_URL = import.meta.env.VITE_LOGOUT_URL ||
    "http://localhost:8081/logout";
// React Query key — used to read/invalidate the session cache
export const CURRENT_USER_QUERY_KEY = ["session", "me"];
export function useCurrentUser() {
    const queryClient = useQueryClient();
    const { data: user, isLoading, isError } = useQuery({
        queryKey: CURRENT_USER_QUERY_KEY,
        queryFn: async () => {
            // GET /api/me — gateway validates the session cookie and returns
            // the authenticated user's profile from Auth0 OIDC id_token claims
            const { data } = await apiClient.get("/api/me");
            return data;
        },
        // ── KEY BFF SETTINGS ────────────────────────────────────────────
        staleTime: Infinity, // Session doesn't expire on a timer — only on 401
        retry: false, // A 401 means "not logged in" — don't retry
        refetchOnWindowFocus: true, // Re-check session when user returns to tab
        // If session expired while tab was in background, this catches it
        // ── ERROR HANDLING ───────────────────────────────────────────────
        // The response interceptor in api.ts fires window event on 401.
        // ProtectedRoute also catches isError to redirect to login.
        // We intentionally do NOT redirect here — that's ProtectedRoute's job.
        // This hook is also used in public components (e.g. LoginPage to
        // check if already authenticated) where we don't want auto-redirect.
    });
    // ── LOGOUT ──────────────────────────────────────────────────────────
    // Redirect browser to gateway's logout endpoint.
    // The gateway:
    //   1. Clears its server-side session
    //   2. Invalidates the session cookie (Set-Cookie with Max-Age=0)
    //   3. Redirects to Auth0's /v2/logout to clear Auth0's SSO session
    //   4. Auth0 redirects back to our app's login page
    const logout = () => {
        // Clear React Query cache immediately so stale user data
        // doesn't flash if user logs back in quickly
        queryClient.clear();
        // Full browser redirect — React state is wiped, which is correct
        // The gateway handles the rest of the logout flow
        window.location.href = LOGOUT_URL;
    };
    // ── LOGIN ────────────────────────────────────────────────────────────
    // Exposed here for convenience — components can call login() directly
    // instead of hardcoding the gateway URL.
    const login = (returnTo) => {
        const url = returnTo
            ? `${LOGIN_URL}?returnTo=${encodeURIComponent(returnTo)}`
            : LOGIN_URL;
        window.location.href = url;
    };
    return {
        user: user ?? null,
        isLoading,
        isError,
        isAuthenticated: !!user && !isError,
        login,
        logout,
    };
}
