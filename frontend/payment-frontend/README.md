# ⚛️ PayFlow Frontend — React Production Patterns Tutorial

> A senior engineer's guide to production React architecture

---

## 🗂️ File-by-File Production Patterns

| File | Pattern | Why It Matters |
|------|---------|---------------|
| `src/types/index.ts` | Central type definitions | Single source of truth for API contract |
| `src/services/api.ts` | Axios interceptors + service layer | Never call HTTP in components |
| `src/store/uiStore.ts` | Zustand global state | Right tool for global UI (not Redux) |
| `src/hooks/usePayments.ts` | React Query custom hooks | Server state management, caching |
| `src/components/auth/AuthSetup.tsx` | Token provider bridge | Auth0 + Axios integration |
| `src/components/auth/ProtectedRoute.tsx` | Route guard | Declarative auth protection |
| `src/components/common/ErrorBoundary.tsx` | Class component (required) | Production crash containment |
| `src/components/payment/CheckoutForm.tsx` | React Hook Form + Zod | Zero-boilerplate validated forms |
| `src/components/payment/PaymentHistory.tsx` | React Query + optimistic | Table with live data + modals |
| `src/components/layout/AppLayout.tsx` | React Router Outlet pattern | Persistent layout, no flash |
| `src/pages/index.tsx` | Thin page orchestrators | Pages compose, don't logic |
| `src/App.tsx` | Provider composition + routes | All routing in one place |

---

## 🏃 Quick Start

```bash
# 1. Install dependencies
npm install

# 2. Configure environment
cp .env.example .env.local
# Edit .env.local with your Auth0 credentials

# 3. Start development server
npm run dev
# App runs at http://localhost:3000
# Auto-proxies /api/* to http://localhost:8081 (API Gateway)

# 4. Build for production
npm run build
```

---

## 🧠 Key Architecture Decisions

### State Management Strategy
```
┌──────────────────────────────────────────────────┐
│              STATE DECISION TREE                  │
│                                                   │
│  Is it server data? (payments, users, orders)     │
│    → React Query (useQuery / useMutation)         │
│                                                   │
│  Is it truly global UI? (theme, redirecting)     │
│    → Zustand store                               │
│                                                   │
│  Is it form state?                               │
│    → React Hook Form                             │
│                                                   │
│  Is it local component state?                    │
│    → useState / useReducer                       │
└──────────────────────────────────────────────────┘
```

### The Auth0 + Axios Bridge Pattern
```
React component
  → calls usePayments() hook
    → React Query calls paymentApi.getHistory()
      → axios interceptor fires
        → calls getTokenSilently() (registered in AuthSetup)
          → Auth0 returns fresh JWT (or silently refreshes)
        → attaches "Authorization: Bearer <token>" header
      → request goes to API Gateway
        → Gateway forwards token to Quarkus service
          → Quarkus validates JWT against Auth0 JWKS
```

### React Query Cache Invalidation
```typescript
// After a successful refund mutation:
queryClient.invalidateQueries({ queryKey: ["payments", "history"] });

// This tells React Query: "this cached data is stale"
// On next render, it triggers a background refetch
// UI shows stale data immediately, then updates — seamless UX
```

### Error Boundary Placement Strategy
```
App
├── ErrorBoundary (catches everything below)
│   ├── Route: /dashboard → DashboardPage (no extra boundary — low risk)
│   └── Route: /history → ErrorBoundary → HistoryPage (table is complex)
```

---

## 🔐 Auth0 Setup for This App

1. Create a **Single Page Application** in Auth0 Dashboard
2. Set **Allowed Callback URLs**: `http://localhost:3000`
3. Set **Allowed Logout URLs**: `http://localhost:3000`
4. Set **Allowed Web Origins**: `http://localhost:3000`
5. Create an **API** in Auth0 (this is the audience)
6. Copy credentials to `.env.local`

**PKCE Flow (what happens):**
```
1. User clicks "Sign in with Auth0"
2. Browser redirects to Auth0 login page
3. User authenticates (username/password, social, etc.)
4. Auth0 redirects back to localhost:3000 with auth code
5. Auth0 SDK exchanges code for tokens (PKCE, no client secret in browser)
6. getAccessTokenSilently() returns JWT for API calls
7. Tokens stored in localStorage (configurable)
8. Refresh tokens rotate silently — user stays logged in
```

---

## 📦 Why These Libraries?

| Library | Replaced | Reason |
|---------|----------|--------|
| **Vite** | Create React App | 10-100x faster, modern ESM |
| **React Query** | useEffect + useState + Redux | Server state management built for HTTP |
| **Zustand** | Redux / Context API | 10x less code, same power |
| **React Hook Form** | Controlled form state | Uncontrolled = no re-renders on keystroke |
| **Zod** | Manual validation / Yup | TypeScript-first, infers types |
| **Axios** | fetch() | Interceptors, instance config, better errors |
| **Auth0 React SDK** | Custom JWT handling | PKCE, refresh rotation, all handled |
| **date-fns** | moment.js | Tree-shakeable, 300KB smaller |

---

## 🧪 Testing (Production Setup)

```bash
# Install testing deps (not included to keep focus on patterns)
npm install -D vitest @testing-library/react @testing-library/user-event msw

# Unit test a custom hook
import { renderHook, waitFor } from '@testing-library/react';
import { usePaymentHistory } from '../hooks/usePayments';

test('fetches payment history', async () => {
  const { result } = renderHook(() => usePaymentHistory(0), { wrapper });
  await waitFor(() => expect(result.current.isSuccess).toBe(true));
});

# Mock API with MSW (Mock Service Worker)
# Intercepts actual network requests — no axios mocking needed
```

---

## 🚀 Production Checklist

- [ ] `.env.local` has real Auth0 credentials
- [ ] Auth0 URLs updated for production domain
- [ ] `VITE_API_GATEWAY_URL` points to production gateway
- [ ] Error monitoring wired up (Sentry: add to ErrorBoundary.componentDidCatch)
- [ ] Content Security Policy headers configured on server
- [ ] `npm run build` succeeds without TypeScript errors
- [ ] Bundle analyzed: `npx vite-bundle-visualizer`
