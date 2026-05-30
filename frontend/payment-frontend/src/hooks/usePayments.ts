import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { useCallback, useEffect, useRef } from "react";
import toast from "react-hot-toast";
import { paymentApi } from "../services/api";
import { ApiError, OrderRequest, RazorpayPaymentResponse, RefundRequest } from "../types";
import { useUIStore } from "../store/uiStore";
import apiClient from "../services/api";
import { AppUser } from "../types";


const RAZORPAY_SCRIPT_URL = "https://checkout.razorpay.com/v1/checkout.js";

// Load the Razorpay checkout.js script dynamically
function loadRazorpayScript(): Promise<boolean> {
  return new Promise((resolve) => {
    // Already loaded
    if (window.Razorpay) return resolve(true);

    // Already in DOM but not yet loaded
    const existing = document.querySelector(`script[src="${RAZORPAY_SCRIPT_URL}"]`);
    if (existing) {
      existing.addEventListener("load", () => resolve(true));
      existing.addEventListener("error", () => resolve(false));
      return;
    }

    // Create and append script tag
    const script = document.createElement("script");
    script.src = RAZORPAY_SCRIPT_URL;
    script.async = true;
    script.onload = () => resolve(true);
    script.onerror = () => resolve(false);
    document.body.appendChild(script);
  });
}

// ── MAIN HOOK ─────────────────────────────────────────────────────
export function useRazorpayCheckout() {
  const queryClient = useQueryClient();
  const setProcessing = useUIStore((s) => s.setRedirecting); // reuse as "processing" flag
  const razorpayRef = useRef<InstanceType<typeof window.Razorpay> | null>(null);

  // Pre-load the Razorpay script when hook mounts
  // This avoids script load delay when user clicks "Pay"
  useEffect(() => {
    loadRazorpayScript();
  }, []);

  const mutation = useMutation<void, ApiError, OrderRequest & { userName?: string; userEmail?: string }>({
    mutationFn: async ({ userName, userEmail, ...orderRequest }) => {
      // ── 1. ENSURE SCRIPT LOADED ────────────────────────────────
      const scriptLoaded = await loadRazorpayScript();
      if (!scriptLoaded || !window.Razorpay) {
        throw { error: "script_error", message: "Could not load Razorpay checkout. Please try again.", status: 0 } as ApiError;
      }

      // ── 2. CREATE ORDER (Step 1) ───────────────────────────────
      setProcessing(true);
      const order = await paymentApi.createOrder(orderRequest);
      setProcessing(false);

      // ── 3. OPEN RAZORPAY POPUP ─────────────────────────────────
      return new Promise<void>((resolve, reject) => {
        const options = {
          key: order.keyId,
          amount: order.amount,       // in paise — Razorpay displays ₹ automatically
          currency: order.currency,
          name: "PayFlow",            // Your company/app name shown in popup
          description: orderRequest.productName,
          order_id: order.razorpayOrderId,

          handler: async (response: RazorpayPaymentResponse) => {
            try {
              setProcessing(true);
              // ── 4. VERIFY PAYMENT (Step 2) ───────────────────────
              await paymentApi.verifyPayment({
                razorpayPaymentId: response.razorpay_payment_id,
                razorpayOrderId: response.razorpay_order_id,
                razorpaySignature: response.razorpay_signature,
              });

              // ── 5. REFRESH HISTORY ───────────────────────────────
              queryClient.invalidateQueries({ queryKey: ["payments", "history"] });
              toast.success("Payment successful! ✓");
              resolve();
            } catch (err) {
              reject(err);
            } finally {
              setProcessing(false);
            }
          },

          // Pre-fill user details if available (improves UX)
          prefill: {
            name: userName ?? "",
            email: userEmail ?? "",
          },

          theme: {
            color: "#7fff6e",   // Match our accent color
          },

          modal: {
            // Called when user closes the popup without paying
            ondismiss: () => {
              setProcessing(false);
              toast("Payment cancelled", { icon: "◌" });
              reject({ error: "dismissed", message: "Payment cancelled by user", status: 0 });
            },
            escape: true,      // Allow Escape key to close
          },
        };

        razorpayRef.current = new window.Razorpay(options);
        razorpayRef.current.open();
      });
    },

    onError: (error: ApiError) => {
      setProcessing(false);
      if (error.error !== "dismissed") {
        toast.error(error.message || "Payment failed");
      }
    },
  });

  return mutation;
}

// ── PAYMENT HISTORY ───────────────────────────────────────────────
export const QUERY_KEYS = {
  paymentHistory: (page: number) => ["payments", "history", page] as const,
};

export function usePaymentHistory(page = 0) {
  return useQuery({
    queryKey: QUERY_KEYS.paymentHistory(page),
    queryFn: () => paymentApi.getHistory(page),
    staleTime: 1000 * 60 * 2,
    retry: 2,
  });
}

// ── REFUND ────────────────────────────────────────────────────────
export function useRefundMutation() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (request: RefundRequest) => paymentApi.refund(request),
    onSuccess: () => {
      toast.success("Refund processed successfully");
      queryClient.invalidateQueries({ queryKey: ["payments", "history"] });
    },
    onError: (error: ApiError) => {
      toast.error(error.message || "Refund failed");
    },
  });
}

// ── CURRENT USER (BFF) ────────────────────────────────────────────
export const CURRENT_USER_QUERY_KEY = ["session", "me"] as const;

function getEnv(key: string): string {
  return (window as any).__ENV__?.[key]
    ?? import.meta.env[key]
    ?? '';
}

const LOGIN_URL = getEnv('VITE_LOGIN_URL') || "http://localhost:8081/oauth2/authorization/auth0";
const LOGOUT_URL = getEnv('VITE_LOGOUT_URL') || "http://localhost:8081/logout";

export function useCurrentUser() {
  const queryClient = useQueryClient();

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

  const logout = useCallback(() => {
    queryClient.clear();
    window.location.href = LOGOUT_URL;
  }, [queryClient]);

  const login = useCallback((returnTo?: string) => {
    window.location.href = returnTo
      ? `${LOGIN_URL}?returnTo=${encodeURIComponent(returnTo)}`
      : LOGIN_URL;
  }, []);

  return {
    user: user ?? null,
    isLoading,
    isError,
    isAuthenticated: !!user && !isError,
    login,
    logout,
  };
}
