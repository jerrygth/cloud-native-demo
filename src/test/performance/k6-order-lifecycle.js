import http from 'k6/http';
import { check, sleep } from 'k6';
import crypto from 'k6/crypto';

export const options = {
  scenarios: {
    lifecycle: {
      executor: 'ramping-vus',
      startVUs: 0,
      stages: [
        { duration: '30s', target: 20 },
        { duration: '8m',  target: 80 },
        { duration: '30s', target: 0 },
      ],
    },
  },
  thresholds: {
    http_req_failed: ['rate<0.01'],
    http_req_duration: ['p(95)<1500'],
  },
};

const BASE = 'http://localhost:8081';
const WEBHOOK_SECRET = 'webhook_secret_cloud_demo';
const RAZORPAY_SECRET = 'ZWvLvFMMqofp66XRuD2cgW7v';
const XSRF_TOKEN = 'a8af62fa-b1ce-4e55-979b-XXXXXXXX';
const SESSION = '108224d1-dfb2-46d4-9612-XXXXXX';

const defaultHeaders = {
  'Content-Type': 'application/json',
  'X-XSRF-TOKEN': XSRF_TOKEN,
  'Cookie': `XSRF-TOKEN=${XSRF_TOKEN}; SESSION=${SESSION}`,
};

export default function () {

  // ── 1. CREATE ORDER ─────────────────────────────────────────
  const orderRes = http.post(
    `${BASE}/api/payments/order`,
    JSON.stringify({
      productName: 'Load Test Plan',
      amount: 49900,
      currency: 'INR',
      receipt: `rcpt-${__VU}-${__ITER}-${Date.now()}`,
    }),
    {
      headers: defaultHeaders,
      tags: { step: 'create_order' },
    }
  );

  const orderCreated = check(orderRes, { 'order created': (r) => r.status === 201 });

  // Guard against JSON parse errors on API failure
  if (!orderCreated || !orderRes.body) {
    return;
  }

  const order = JSON.parse(orderRes.body);
  const paymentId = `pay_LT${__VU}${__ITER}${Date.now()}`;

  // Calculate signature using k6/crypto module
  const signaturePayload = `${order.razorpayOrderId}|${paymentId}`;
  const signature = crypto.hmac('sha256', RAZORPAY_SECRET, signaturePayload, 'hex');

  // Simulate user filling in the Razorpay popup (1-3s)
  sleep(Math.random() * 2 + 1);

  // ── 2. VERIFY PAYMENT ───────────────────────────────────────
  const verifyRes = http.post(
    `${BASE}/api/payments/verify`,
    JSON.stringify({
      razorpayPaymentId: paymentId,
      razorpayOrderId: order.razorpayOrderId,
      razorpaySignature: signature,
    }),
    {
      headers: defaultHeaders,
      tags: { step: 'verify_payment' },
    }
  );

  check(verifyRes, { 'verify 200 or 404': (r) => r.status === 200 || r.status === 404 });

  // Async delay before webhook typically arrives
  sleep(0.5);

  // ── 3. WEBHOOK (simulates Razorpay async confirmation) ───
  const webhookPayload = JSON.stringify({
    entity: 'event',
    event: 'payment.captured',
    payload: {
      payment: {
        entity: {
          id: paymentId,
          order_id: order.razorpayOrderId,
          amount: 49900,
          currency: 'INR',
          status: 'captured',
          method: 'upi',
          captured: true,
        },
      },
    },
  });

  // Calculate HMAC using k6 native crypto API
  const webhookSignature = crypto.hmac('sha256', WEBHOOK_SECRET, webhookPayload, 'hex');

  const webhookRes = http.post(`${BASE}/api/payments/webhook`, webhookPayload, {
    headers: {
      'Content-Type': 'application/json',
      'X-Razorpay-Signature': webhookSignature,
      'X-XSRF-TOKEN': XSRF_TOKEN,
      'Cookie': `'XSRF-TOKEN=${XSRF_TOKEN}; SESSION=${SESSION}'`
    },
    tags: { step: 'webhook' },
  });

  check(webhookRes, { 'webhook 200': (r) => r.status === 200 });
}