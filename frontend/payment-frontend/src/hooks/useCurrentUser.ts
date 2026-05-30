import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { useEffect } from "react";
import apiClient from "../services/api";
import { AppUser, UpdatePreferencesRequest, UpdateProfileRequest } from "../types";
import { useUIStore } from "../store/uiStore";

function getEnv(key: string): string {
  return (window as any).__ENV__?.[key]
    ?? import.meta.env[key]
    ?? '';
}

const LOGIN_URL  = getEnv('VITE_LOGIN_URL')  || "http://localhost:8081/oauth2/authorization/auth0";
const LOGOUT_URL = getEnv('VITE_LOGOUT_URL') || "http://localhost:8081/logout";

export const CURRENT_USER_QUERY_KEY = ["session", "me"] as const;

// ── useCurrentUser ────────────────────────────────────────────────
export function useCurrentUser() {
  const queryClient = useQueryClient();
  const { setTheme } = useUIStore();

  const { data: user, isLoading, isError } = useQuery<AppUser, Error>({
    queryKey: CURRENT_USER_QUERY_KEY,
    queryFn: async () => {
      const { data } = await apiClient.get<AppUser>("/api/me");
      return data;
    },
    staleTime: Infinity,
    retry: false,
    refetchOnWindowFocus: true,
  });

  // Sync server-side theme preference into Zustand on login.
  // If user saved "light" theme on another device, it restores automatically.
  useEffect(() => {
    if (user?.preferences?.theme) {
      setTheme(user.preferences.theme);
    }
  }, [user?.preferences?.theme, setTheme]);

  const logout = () => {
    queryClient.clear();
    window.location.href = LOGOUT_URL;
  };

  const login = (returnTo?: string) => {
    window.location.href = returnTo
      ? `${LOGIN_URL}?returnTo=${encodeURIComponent(returnTo)}`
      : LOGIN_URL;
  };

  return {
    user:           user ?? null,
    isLoading,
    isError,
    isAuthenticated: !!user && !isError,
    // True on first login — use to show an onboarding prompt if needed
    isFirstLogin:   user ? !user.profileComplete : false,
    login,
    logout,
  };
}

// ── useUpdateProfile ──────────────────────────────────────────────
// PATCH /api/v1/users/me — updates firstName, lastName, phoneNumber.
// FIX: field names now match UpdateProfileRequest (firstName/lastName,
// not displayName which doesn't exist in the UserAccount service).
export function useUpdateProfile() {
  const queryClient = useQueryClient();

  return useMutation<AppUser, Error, UpdateProfileRequest>({
    mutationFn: async (request) => {
      const { data } = await apiClient.patch<AppUser>("/api/v1/users/me", request);
      return data;
    },
    onSuccess: (updatedUser) => {
      // Update the cached /api/me response with new name fields.
      // Recompute the composed "name" field so the navbar stays in sync.
      queryClient.setQueryData(CURRENT_USER_QUERY_KEY, (old: AppUser | undefined) => {
        if (!old) return old;
        const firstName = updatedUser.firstName ?? old.firstName;
        const lastName  = updatedUser.lastName  ?? old.lastName;
        return {
          ...old,
          firstName,
          lastName,
          phoneNumber: updatedUser.phoneNumber ?? old.phoneNumber,
          // Re-compose the display name
          name: [firstName, lastName].filter(Boolean).join(" ").trim() || old.name,
        };
      });
    },
  });
}

// ── useUpdatePreferences ──────────────────────────────────────────
// PATCH /api/v1/users/me/preferences — persists theme, language, notifications.
// Uses optimistic update: UI changes instantly, server is updated in background.
// On server error the previous preference is rolled back.
export function useUpdatePreferences() {
  const queryClient = useQueryClient();
  const { setTheme } = useUIStore();

  return useMutation<AppUser, Error, UpdatePreferencesRequest>({
    mutationFn: async (request) => {
      const { data } = await apiClient.patch<AppUser>(
        "/api/v1/users/me/preferences",
        request
      );
      return data;
    },

    onMutate: async (newPrefs) => {
      await queryClient.cancelQueries({ queryKey: CURRENT_USER_QUERY_KEY });
      const previous = queryClient.getQueryData<AppUser>(CURRENT_USER_QUERY_KEY);

      if (previous) {
        queryClient.setQueryData<AppUser>(CURRENT_USER_QUERY_KEY, {
          ...previous,
          preferences: { ...previous.preferences, ...newPrefs },
        });
      }
      return { previous };
    },

    onSuccess: (updatedUser) => {
      queryClient.setQueryData(CURRENT_USER_QUERY_KEY, (old: AppUser | undefined) =>
        old ? { ...old, preferences: updatedUser.preferences ?? old.preferences } : old
      );
    },

    onError: (_err, _vars, context: any) => {
      if (context?.previous) {
        queryClient.setQueryData(CURRENT_USER_QUERY_KEY, context.previous);
        if (context.previous.preferences?.theme) {
          setTheme(context.previous.preferences.theme);
        }
      }
    },
  });
}

// ── useDeleteAccount ──────────────────────────────────────────────
// DELETE /api/v1/users/me — GDPR soft delete.
// Clears session and redirects to logout after deletion.
export function useDeleteAccount() {
  const queryClient = useQueryClient();

  return useMutation<void, Error, void>({
    mutationFn: async () => {
      await apiClient.delete("/api/v1/users/me");
    },
    onSuccess: () => {
      queryClient.clear();
      window.location.href = LOGOUT_URL;
    },
  });
}