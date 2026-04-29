import { jsx as _jsx, jsxs as _jsxs, Fragment as _Fragment } from "react/jsx-runtime";
import { zodResolver } from "@hookform/resolvers/zod";
import { useForm } from "react-hook-form";
import { z } from "zod";
import { useRazorpayCheckout } from "../../hooks/usePayments";
import { useCurrentUser } from "../../hooks/usePayments";
import { useUIStore } from "../../store/uiStore";
// ═══════════════════════════════════════════════════════════════════
// CHECKOUT FORM — Razorpay Edition
// ═══════════════════════════════════════════════════════════════════
//
// KEY DIFFERENCE vs Stripe:
//
//   Stripe:
//     - User fills form → clicks Pay → browser redirects to Stripe
//     - User is gone from your app until Stripe redirects back
//     - Simple: one mutation, one redirect
//
//   Razorpay:
//     - User fills form → clicks Pay → Razorpay popup appears ON your page
//     - User pays inline (UPI QR, card, netbanking) — never leaves
//     - More complex: createOrder() → popup → verifyPayment()
//     - Better UX: stays on your page, instant success feedback
//
// CURRENCY NOTE:
//   Razorpay primarily serves India — default currency is INR.
//   Amount is in PAISE (not rupees): ₹100 = 10000 paise.
//   We collect rupees from the user and multiply by 100 before sending.
//
// UPI SUPPORT:
//   Razorpay popup automatically shows UPI, cards, netbanking, wallets.
//   No extra config needed — all payment methods are available by default.
// ═══════════════════════════════════════════════════════════════════
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
export function CheckoutForm() {
    const { mutate: pay, isPending } = useRazorpayCheckout();
    const { user } = useCurrentUser();
    const isProcessing = useUIStore((s) => s.isRedirecting);
    const { register, handleSubmit, formState: { errors, isValid, dirtyFields }, reset, } = useForm({
        resolver: zodResolver(checkoutSchema),
        mode: "onChange",
        defaultValues: {
            currency: "INR",
            receipt: `rcpt-${Date.now()}`,
        },
    });
    const onSubmit = (data) => {
        pay({
            productName: data.productName,
            // Convert rupees → paise (Razorpay uses smallest currency unit)
            amount: Math.round(data.amount * 100),
            currency: data.currency,
            receipt: data.receipt,
            userName: user?.name,
            userEmail: user?.email,
        });
    };
    const isBusy = isPending || isProcessing;
    return (_jsxs("div", { className: "checkout-form", children: [_jsxs("div", { className: "checkout-form__header", children: [_jsx("div", { className: "checkout-form__icon", children: "\u20B9" }), _jsx("h2", { children: "New Payment" }), _jsx("p", { children: "Pay via UPI, Card, Netbanking & more" })] }), _jsxs("form", { onSubmit: handleSubmit(onSubmit), noValidate: true, children: [_jsxs("div", { className: "form-group", children: [_jsx("label", { htmlFor: "productName", className: "form-label", children: "Product / Service Name" }), _jsx("input", { id: "productName", className: `form-input ${errors.productName ? "form-input--error" : ""} ${dirtyFields.productName && !errors.productName ? "form-input--valid" : ""}`, placeholder: "e.g. Premium Plan", ...register("productName") }), errors.productName && (_jsx("span", { className: "form-error", children: errors.productName.message }))] }), _jsxs("div", { className: "form-row", children: [_jsxs("div", { className: "form-group", children: [_jsx("label", { htmlFor: "amount", className: "form-label", children: "Amount (\u20B9)" }), _jsxs("div", { className: "input-prefix-wrapper", children: [_jsx("span", { className: "input-prefix", children: "\u20B9" }), _jsx("input", { id: "amount", type: "number", step: "0.01", min: "1", className: `form-input form-input--prefixed ${errors.amount ? "form-input--error" : ""}`, placeholder: "499", ...register("amount", { valueAsNumber: true }) })] }), errors.amount && (_jsx("span", { className: "form-error", children: errors.amount.message }))] }), _jsxs("div", { className: "form-group", children: [_jsx("label", { htmlFor: "currency", className: "form-label", children: "Currency" }), _jsxs("select", { id: "currency", className: "form-input form-select", ...register("currency"), children: [_jsx("option", { value: "INR", children: "INR \u20B9" }), _jsx("option", { value: "USD", children: "USD $" })] })] })] }), _jsxs("div", { className: "form-group", children: [_jsx("label", { htmlFor: "receipt", className: "form-label", children: "Order / Receipt ID" }), _jsx("input", { id: "receipt", className: `form-input ${errors.receipt ? "form-input--error" : ""}`, placeholder: "rcpt-12345", ...register("receipt") }), errors.receipt && (_jsx("span", { className: "form-error", children: errors.receipt.message }))] }), _jsxs("div", { className: "payment-methods-banner", children: [_jsx("span", { className: "payment-method-chip", children: "\uD83D\uDD35 UPI" }), _jsx("span", { className: "payment-method-chip", children: "\uD83D\uDCB3 Cards" }), _jsx("span", { className: "payment-method-chip", children: "\uD83C\uDFE6 Netbanking" }), _jsx("span", { className: "payment-method-chip", children: "\uD83D\uDC5B Wallets" }), _jsx("span", { className: "payment-method-chip", children: "\uD83D\uDCF1 EMI" })] }), _jsxs("div", { className: "checkout-form__test-card", children: [_jsx("span", { className: "badge badge--info", children: "Test Mode" }), _jsxs("span", { children: ["UPI: ", _jsx("code", { children: "success@razorpay" }), " \u00B7 Card:", " ", _jsx("code", { children: "4111 1111 1111 1111" })] })] }), _jsxs("div", { className: "checkout-form__actions", children: [_jsx("button", { type: "button", className: "btn btn--ghost", onClick: () => reset(), disabled: isBusy, children: "Reset" }), _jsx("button", { type: "submit", className: `btn btn--primary ${isBusy ? "btn--loading" : ""}`, disabled: !isValid || isBusy, children: isProcessing ? (_jsxs(_Fragment, { children: [_jsx("span", { className: "spinner spinner--sm" }), " Creating order..."] })) : isPending ? (_jsxs(_Fragment, { children: [_jsx("span", { className: "spinner spinner--sm" }), " Processing..."] })) : (_jsx(_Fragment, { children: "Pay with Razorpay \u2192" })) })] })] })] }));
}
