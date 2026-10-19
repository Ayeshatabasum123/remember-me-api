# Community Burial Fund — Donations API & Stripe Integration Guide

This guide details the complete Community Burial Fund Donations API, Stripe payment flow, webhook handling, immutable fund ledger, Flutter PaymentSheet integration contract, and testing instructions with the Stripe CLI.

---

## 1. Architecture & Design Principles

1. **Shared Fund Model**: All donations flow directly into the single shared **Community Burial Fund**. Loved one dedications are purely commemorative and never allocate or ring-fence money to individual deceased records or grave plots.
2. **Integer Pence Accounting**: All monetary values (`amount_pence`, `fee_pence`, `net_amount_pence`, `refunded_amount_pence`, `running_balance_pence`) are strictly stored as 64-bit integers (`Long`) in pence. Floating-point calculations are strictly forbidden. The currency is locked to `GBP` (`£`).
3. **Double-Safety & Idempotency**:
   - `Idempotency-Key` is supported for PaymentIntent creation and passed directly to Stripe's `RequestOptions`.
   - Webhook event idempotency is guaranteed via the `payment_events` table (`stripe_event_id` unique constraint).
   - Receipt generation is unique per donation (`donation_id` unique on `donation_receipts`).
   - Webhooks are the sole authority for payment success, failure, cancellations, refunds, and disputes.
4. **Immutable Fund Ledger**:
   - `fund_ledger` is strictly append-only.
   - When a donation settles, a `DONATION_SETTLEMENT` entry is posted net of Stripe processing fees.
   - When a refund occurs, a `DONATION_REFUND` entry adjusts the running balance downwards.
   - Available fund balance = $\sum (\text{net\_amount\_pence})$ across all ledger rows.

---

## 2. Environment Variables & Credentials

Configure the following variables in your environment or `application.yml` / `application-prod.yml`:

| Variable | Description | Example / Default |
|---|---|---|
| `STRIPE_SECRET_KEY` | Stripe Restricted/Secret Key for backend | `sk_test_51...` |
| `STRIPE_PUBLISHABLE_KEY` | Stripe Publishable Key for Flutter mobile app | `pk_test_51...` |
| `STRIPE_WEBHOOK_SECRET` | Signing secret for Stripe Webhooks | `whsec_...` |

---

## 3. Database Schema & Tables

Idempotent migrations are registered in `DatabaseSchemaMigration.java`. The schema adds:

### `donations`
- `id` (BIGSERIAL PK)
- `user_id` (BIGINT FK to `users(id)`)
- `amount_pence` (BIGINT NOT NULL)
- `currency` (VARCHAR(3) NOT NULL DEFAULT 'GBP')
- `payment_status` (VARCHAR(32): `PENDING`, `PROCESSING`, `SUCCEEDED`, `FAILED`, `CANCELLED`, `REFUNDED`, `PARTIALLY_REFUNDED`, `DISPUTED`)
- `settlement_status` (VARCHAR(32): `PENDING`, `SETTLED`, `FAILED`)
- `fee_pence` (BIGINT NOT NULL DEFAULT 0)
- `net_amount_pence` (BIGINT NOT NULL DEFAULT 0)
- `refunded_amount_pence` (BIGINT NOT NULL DEFAULT 0)
- `stripe_payment_intent_id` (VARCHAR(255) UNIQUE)
- `stripe_charge_id` (VARCHAR(255))
- `stripe_balance_transaction_id` (VARCHAR(255))
- `stripe_customer_id` (VARCHAR(255))
- `idempotency_key` (VARCHAR(255) UNIQUE)
- `client_secret` (VARCHAR(512))
- `donor_email` (VARCHAR(255))
- `donor_name` (VARCHAR(255))
- `created_at`, `updated_at`, `succeeded_at`, `settled_at` (TIMESTAMP)

### `donation_dedications`
- `id` (BIGSERIAL PK)
- `donation_id` (BIGINT FK to `donations(id)` ON DELETE CASCADE)
- `dedication_name` (VARCHAR(255) NOT NULL)
- `deceased_person_id` (BIGINT, optional FK reference)
- `memorial_id` (BIGINT, optional FK reference)
- `message` (TEXT, optional tribute/prayer)
- `created_at` (TIMESTAMP)

### `payment_events`
- `id` (BIGSERIAL PK)
- `stripe_event_id` (VARCHAR(255) NOT NULL UNIQUE)
- `event_type` (VARCHAR(100) NOT NULL)
- `stripe_payment_intent_id` (VARCHAR(255))
- `stripe_charge_id` (VARCHAR(255))
- `status` (VARCHAR(32): `PROCESSED`, `IGNORED`, `FAILED`)
- `payload` (TEXT)
- `error_message` (TEXT)
- `created_at` (TIMESTAMP)

### `donation_receipts`
- `id` (BIGSERIAL PK)
- `donation_id` (BIGINT NOT NULL UNIQUE FK to `donations(id)`)
- `receipt_number` (VARCHAR(64) NOT NULL UNIQUE, e.g. `RCP-20261019-XXXXXX`)
- `donor_name` (VARCHAR(255) NOT NULL)
- `donor_email` (VARCHAR(255) NOT NULL)
- `amount_pence` (BIGINT NOT NULL)
- `currency` (VARCHAR(3) NOT NULL DEFAULT 'GBP')
- `payment_method` (VARCHAR(50))
- `stripe_charge_id` (VARCHAR(255))
- `fund_name` (VARCHAR(255) NOT NULL DEFAULT 'Community Burial Fund')
- `receipt_url` (VARCHAR(512))
- `issued_at` (TIMESTAMP)

### `fund_ledger`
- `id` (BIGSERIAL PK)
- `donation_id` (BIGINT)
- `entry_type` (VARCHAR(50): `DONATION_SETTLEMENT`, `DONATION_REFUND`, `DISPUTE_ADJUSTMENT`, `BURIAL_SUPPORT_DISBURSEMENT`, `MANUAL_ADJUSTMENT`)
- `gross_amount_pence` (BIGINT NOT NULL)
- `fee_pence` (BIGINT NOT NULL DEFAULT 0)
- `net_amount_pence` (BIGINT NOT NULL)
- `currency` (VARCHAR(3) NOT NULL DEFAULT 'GBP')
- `running_balance_pence` (BIGINT NOT NULL)
- `stripe_balance_transaction_id` (VARCHAR(255))
- `reference` (VARCHAR(255) NOT NULL)
- `description` (TEXT NOT NULL)
- `created_at` (TIMESTAMP)

---

## 4. API Endpoints Reference

### 4.1. Get Donation Options & Presets
- **Method**: `GET /api/v1/donations/options`
- **Auth**: Public or Bearer Token
- **Response**:
```json
{
  "success": true,
  "message": "Donation options retrieved successfully",
  "data": {
    "currency": "GBP",
    "currencySymbol": "£",
    "presetAmounts": [
      { "amountPence": 500, "displayLabel": "£5" },
      { "amountPence": 1000, "displayLabel": "£10" },
      { "amountPence": 5000, "displayLabel": "£50" }
    ],
    "minCustomAmountPence": 100,
    "maxCustomAmountPence": 500000,
    "sharedFundExplanation": "Every contribution directly strengthens our single Community Burial Fund, helping ensure that every community member in need receives a respectful burial. Loved one dedications serve as meaningful commemorative tributes but never restrict or allocate funds to individuals or graves.",
    "stripePublishableKey": "pk_test_..."
  }
}
```

---

### 4.2. Create Stripe PaymentIntent
- **Method**: `POST /api/v1/donations/payment-intent`
- **Auth**: Bearer Token required
- **Headers**: `Idempotency-Key: <unique-uuid-or-key>` (optional, or in payload)
- **Request Body**:
```json
{
  "amountPence": 1000,
  "currency": "GBP",
  "idempotencyKey": "idem-flutter-8f3a9",
  "dedications": [
    {
      "dedicationName": "Haji Mohammed",
      "deceasedPersonId": 14,
      "message": "May Allah grant him Jannatul Firdaus."
    }
  ]
}
```
- **Response**:
```json
{
  "success": true,
  "message": "Payment intent created successfully",
  "data": {
    "donationId": 101,
    "clientSecret": "pi_3Mtwx72eZvKYlo2C01n0D012_secret_...",
    "paymentIntentId": "pi_3Mtwx72eZvKYlo2C01n0D012",
    "amountPence": 1000,
    "currency": "GBP",
    "status": "PENDING",
    "idempotencyKey": "idem-flutter-8f3a9"
  }
}
```

---

### 4.3. Donor History & Receipt Endpoints
- `GET /api/v1/me/donations?page=0&size=10`
  - Paginated list of donations belonging strictly to the authenticated donor.
- `GET /api/v1/me/donations/{id}`
  - Returns donation status and details. Enforces ownership: returns `403 FORBIDDEN` if not owned by caller.
- `GET /api/v1/me/donations/{id}/receipt`
  - Returns the official receipt for a donation strictly to its donor (`403` if not owner, `404` if not yet confirmed/settled).
```json
{
  "success": true,
  "message": "Receipt retrieved successfully",
  "data": {
    "receiptNumber": "RCP-20261019-A8F291",
    "donationId": 101,
    "donorName": "Amina Begum",
    "donorEmail": "amina@example.com",
    "amountPence": 1000,
    "amountFormatted": "£10.00",
    "currency": "GBP",
    "paymentMethod": "card",
    "fundName": "Community Burial Fund",
    "receiptUrl": "https://pay.stripe.com/receipts/...",
    "issuedAt": "2026-10-19T18:24:27"
  }
}
```

---

### 4.4. Stripe Webhook Receiver
- **Method**: `POST /api/v1/webhooks/stripe`
- **Auth**: Public, authenticated via `Stripe-Signature` cryptographic header
- **Handled Events**:
  - `payment_intent.succeeded` -> Sets status to `SUCCEEDED`, calculates fee, sets `SETTLED`, records ledger settlement, issues receipt.
  - `payment_intent.payment_failed` -> Sets status to `FAILED` (if not already succeeded).
  - `payment_intent.canceled` -> Sets status to `CANCELLED`.
  - `charge.refunded` -> Sets status to `REFUNDED` / `PARTIALLY_REFUNDED`, records refund in ledger.
  - `charge.dispute.created` -> Sets status to `DISPUTED`, adjusts ledger.
  - `charge.dispute.closed` -> If won, restores status and re-credits fund ledger.

---

### 4.5. Community Burial Fund Summary
- **Method**: `GET /api/v1/fund/summary`
- **Auth**: Public
- **Response**:
```json
{
  "success": true,
  "message": "Fund summary retrieved successfully",
  "data": {
    "currency": "GBP",
    "currencySymbol": "£",
    "totalSettledDonationsCount": 142,
    "totalGrossPence": 750000,
    "totalFeesPence": 21500,
    "totalRefundsPence": 0,
    "availableBalancePence": 728500,
    "availableBalanceFormatted": "£7,285.00",
    "fundPurpose": "The Remember Me Community Burial Fund provides dignified, communal burial assistance to families facing hardship. Every donation directly increases the available fund pool.",
    "generatedAt": "2026-10-19T18:25:20"
  }
}
```

---

## 5. Flutter PaymentSheet Integration Contract

In the Flutter application (`flutter_stripe` package):

```dart
// 1. Fetch options and publishable key
final optionsResponse = await dio.get('/api/v1/donations/options');
Stripe.publishableKey = optionsResponse.data['data']['stripePublishableKey'];

// 2. Request PaymentIntent
final intentResponse = await dio.post(
  '/api/v1/donations/payment-intent',
  data: {
    'amountPence': 1000, // £10.00 in pence
    'currency': 'GBP',
    'idempotencyKey': Uuid().v4(),
    'dedications': [
      {
        'dedicationName': 'Loved One Name',
        'message': 'Rest in peace'
      }
    ]
  },
);

final clientSecret = intentResponse.data['data']['clientSecret'];
final donationId = intentResponse.data['data']['donationId'];

// 3. Initialize & Present PaymentSheet
await Stripe.instance.initPaymentSheet(
  paymentSheetParameters: SetupPaymentSheetParameters(
    paymentIntentClientSecret: clientSecret,
    merchantDisplayName: 'Remember Me Community Burial Fund',
    style: ThemeMode.system,
    appearance: PaymentSheetAppearance(
      colors: PaymentSheetAppearanceColors(
        primary: Color(0xFF1E88E5),
      ),
    ),
  ),
);

// 4. Confirm Payment
try {
  await Stripe.instance.presentPaymentSheet();
  // Payment submitted. Poll /api/v1/me/donations/$donationId to wait for webhook confirmation.
} on StripeException catch (e) {
  // Handle user cancellation or card failure
}
```

---

## 6. Stripe CLI Local Webhook Testing

To test webhooks locally:

1. **Log in to Stripe CLI**:
   ```bash
   stripe login
   ```

2. **Forward Webhook Events to your local backend**:
   ```bash
   stripe listen --forward-to localhost:8080/api/v1/webhooks/stripe
   ```
   The CLI outputs your webhook signing secret: `whsec_...`
   Set this as `STRIPE_WEBHOOK_SECRET` in your local environment.

3. **Trigger Test Events**:
   ```bash
   # Trigger payment succeeded
   stripe trigger payment_intent.succeeded

   # Trigger payment failed
   stripe trigger payment_intent.payment_failed

   # Trigger refund
   stripe trigger charge.refunded
   ```
