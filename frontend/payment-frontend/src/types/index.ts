
export interface UserPreferences {
  theme: "dark" | "light";
  language: string;
  notifications: boolean;
}

export interface AppUser {
  // Auth0 identity (from id_token)
  sub:            string;
  email:          string;
  picture?:       string;
  emailVerified?: boolean;

  // UserAccount service fields (firstName + lastName from User entity)
  firstName:    string;
  lastName:     string;
  phoneNumber?: string;
  status?:      "ACTIVE" | "INACTIVE" | "DELETED";
  name: string;

  // Application state
  profileComplete: boolean;  // false on first login
  preferences:     UserPreferences;
}

// ── UPDATE REQUESTS ───────────────────────────────────────────────
// Matches UpdateProfileRequest in UserAccount service.
// All fields optional — PATCH semantics (only send what changed).
export interface UpdateProfileRequest {
  firstName?:   string;
  lastName?:    string;
  phoneNumber?: string;
}

// Matches UpdatePreferencesRequest in UserAccount service.
export interface UpdatePreferencesRequest {
  theme?:         "dark" | "light";
  language?:      string;
  notifications?: boolean;
}

// ── PAYMENT TYPES ─────────────────────────────────────────────────
export type PaymentStatus =
  | "CREATED" | "AUTHORIZED" | "CAPTURED"
  | "FAILED"  | "REFUNDED"   | "PARTIALLY_REFUNDED";

export interface Payment {
  id:                 number;
  razorpayOrderId:    string;
  razorpayPaymentId:  string | null;
  productName:        string;
  amount:             number;
  currency:           string;
  status:             PaymentStatus;
  paymentMethod:      string | null;
  createdAt:          string;
  capturedAt:         string | null;
}

export interface OrderRequest {
  productName: string;
  amount:      number;
  currency?:   string;
  receipt:     string;
}

export interface OrderResponse {
  razorpayOrderId: string;
  amount:          number;
  currency:        string;
  receipt:         string;
  keyId:           string;
}

export interface VerifyRequest {
  razorpayPaymentId: string;
  razorpayOrderId:   string;
  razorpaySignature: string;
}

export interface VerifyResponse {
  success:   boolean;
  paymentId: number;
  message:   string;
}

export interface RefundRequest {
  paymentId: number;
  amount?:   number;
  speed?:    "normal" | "optimum" | "instant";
}

export interface RefundResponse {
  refundId:       string;
  amountRefunded: number;
  status:         string;
}

export interface ApiError {
  error:   string;
  message: string;
  status:  number;
}

// Razorpay SDK global type
export interface RazorpayOptions {
  key:          string;
  amount:       number;
  currency:     string;
  name:         string;
  description?: string;
  order_id:     string;
  handler:      (response: RazorpayPaymentResponse) => void;
  prefill?:     { name?: string; email?: string; contact?: string };
  theme?:       { color?: string };
  modal?:       { ondismiss?: () => void; escape?: boolean };
}

export interface RazorpayPaymentResponse {
  razorpay_payment_id: string;
  razorpay_order_id:   string;
  razorpay_signature:  string;
}

declare global {
  interface Window {
    Razorpay: new (options: RazorpayOptions) => { open: () => void; close: () => void };
  }
}