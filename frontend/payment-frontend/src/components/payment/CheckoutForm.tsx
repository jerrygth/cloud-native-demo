import { zodResolver } from "@hookform/resolvers/zod";
import { useForm } from "react-hook-form";
import { z } from "zod";
import { useRazorpayCheckout } from "../../hooks/usePayments";
import { useCurrentUser } from "../../hooks/usePayments";
import { useUIStore } from "../../store/uiStore";


const checkoutSchema = z.object({
  productName: z
    .string()
    .min(1, "Product name is required")
    .max(200, "Product name too long"),

  amount: z
    .number({ invalid_type_error: "Amount must be a number" })
    .positive("Amount must be greater than ₹0")
    .max(500000, "Amount cannot exceed ₹5,00,000"),

  currency: z.enum(["INR", "USD"]),

  receipt: z
    .string()
    .min(1, "Receipt/Order ID is required")
    .regex(/^[a-zA-Z0-9_-]+$/, "Only letters, numbers, _ and - allowed"),
});

type CheckoutFormData = z.infer<typeof checkoutSchema>;

export function CheckoutForm() {
  const { mutate: pay, isPending } = useRazorpayCheckout();
  const { user } = useCurrentUser();
  const isProcessing = useUIStore((s) => s.isRedirecting);

  const {
    register,
    handleSubmit,
    formState: { errors, isValid, dirtyFields },
    reset,
  } = useForm<CheckoutFormData>({
    resolver: zodResolver(checkoutSchema),
    mode: "onChange",
    defaultValues: {
      currency: "INR",
      receipt: `Rcpt-${Date.now()}`,
    },
  });

  const onSubmit = (data: CheckoutFormData) => {
    pay({
      productName: data.productName,
      amount: Math.round(data.amount * 100),
      currency: data.currency,
      receipt: data.receipt,
      userName: user?.name,
      userEmail: user?.email,
    });
  };

  const isBusy = isPending || isProcessing;

  return (
    <div className="checkout-form">
      <div className="checkout-form__header">
        <div className="checkout-form__icon">₹</div>
        <h2>New Payment</h2>
        <p>Pay via UPI, Card, Netbanking &amp; more</p>
      </div>

      <form onSubmit={handleSubmit(onSubmit)} noValidate>
        <div className="form-group">
          <label htmlFor="productName" className="form-label">
            Product / Service Name
          </label>
          <input
            id="productName"
            className={`form-input ${errors.productName ? "form-input--error" : ""} ${
              dirtyFields.productName && !errors.productName ? "form-input--valid" : ""
            }`}
            placeholder="e.g. Test Plan"
            {...register("productName")}
          />
          {errors.productName && (
            <span className="form-error">{errors.productName.message}</span>
          )}
        </div>

        <div className="form-row">
          <div className="form-group">
            <label htmlFor="amount" className="form-label">
              Amount (₹)
            </label>
            <div className="input-prefix-wrapper">
              <span className="input-prefix">₹</span>
              <input
                id="amount"
                type="number"
                step="0.01"
                min="1"
                className={`form-input form-input--prefixed ${errors.amount ? "form-input--error" : ""}`}
                placeholder="499"
                {...register("amount", { valueAsNumber: true })}
              />
            </div>
            {errors.amount && (
              <span className="form-error">{errors.amount.message}</span>
            )}
          </div>

          <div className="form-group">
            <label htmlFor="currency" className="form-label">Currency</label>
            <select id="currency" className="form-input form-select" {...register("currency")}>
              <option value="INR">INR ₹</option>
              <option value="USD">USD $</option>
            </select>
          </div>
        </div>

        <div className="form-group">
          <label htmlFor="receipt" className="form-label">
            Order / Receipt ID
          </label>
          <input
            id="receipt"
            className={`form-input ${errors.receipt ? "form-input--error" : ""}`}
            placeholder="rcpt-12345"
            {...register("receipt")}
          />
          {errors.receipt && (
            <span className="form-error">{errors.receipt.message}</span>
          )}
        </div>

        {/* Payment methods banner */}
        <div className="payment-methods-banner">
          <span className="payment-method-chip">🔵 UPI</span>
          <span className="payment-method-chip">💳 Cards</span>
          <span className="payment-method-chip">🏦 Netbanking</span>         
          
        </div>

        {/* Test mode info */}
        <div className="checkout-form__test-card">
          <span className="badge badge--info">Test Mode</span>
          <span>
            UPI: <code>success@razorpay</code> · Card:{" "}
            <code>4111 1111 1111 1111</code>
          </span>
        </div>

        <div className="checkout-form__actions">
          <button
            type="button"
            className="btn btn--ghost"
            onClick={() => reset()}
            disabled={isBusy}
          >
            Reset
          </button>
          <button
            type="submit"
            className={`btn btn--primary ${isBusy ? "btn--loading" : ""}`}
            disabled={!isValid || isBusy}
          >
            {isProcessing ? (
              <><span className="spinner spinner--sm" /> Creating order...</>
            ) : isPending ? (
              <><span className="spinner spinner--sm" /> Processing...</>
            ) : (
              <>Pay with Razorpay →</>
            )}
          </button>
        </div>
      </form>
    </div>
  );
}
