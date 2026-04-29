
CREATE SEQUENCE payments_seq START WITH 1 INCREMENT BY 1;

CREATE TABLE payments (
    id         BIGINT PRIMARY KEY DEFAULT nextval('payments_seq'),
    user_id              VARCHAR(255)    NOT NULL,
    product_name         VARCHAR(200)    NOT NULL,

    -- Razorpay identifiers (populated at different stages of the flow)
    razorpay_order_id    VARCHAR(100)    UNIQUE,         -- Step 1: order created
    razorpay_payment_id  VARCHAR(100)    UNIQUE,         -- Step 2: payment verified

    amount               BIGINT          NOT NULL,        -- in paise (₹1 = 100 paise)
    currency             VARCHAR(3)      NOT NULL DEFAULT 'INR',
    receipt              VARCHAR(100),                    -- your internal reference

    -- Status follows Razorpay lifecycle: CREATED → CAPTURED → REFUNDED
    status               VARCHAR(30)     NOT NULL DEFAULT 'CREATED',

    -- Payment method populated after Razorpay returns payment details
    payment_method       VARCHAR(30),                     -- upi, card, netbanking, wallet, emi

    created_at           TIMESTAMP       NOT NULL DEFAULT NOW(),
    captured_at          TIMESTAMP                        -- null until payment captured
);

ALTER SEQUENCE payments_seq OWNED BY payments.id;


-- Indexes for common queries
CREATE INDEX idx_payments_user_id ON payments(user_id);
CREATE INDEX idx_payments_razorpay_order_id ON payments(razorpay_order_id);
CREATE INDEX idx_payments_status ON payments(status);
CREATE INDEX idx_payments_created_at ON payments(created_at DESC);