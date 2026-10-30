# Checkout configuration

Checkout offers free shipping for every order and does not require a GHN fee
quote or packed-product shipping dimensions. Inventory is held for 15 minutes
by default, and VNPay payment links expire after 15 minutes by default. A VNPay
order is created only after a signed successful payment result is verified.
Canceling/returning from VNPay releases the inventory hold immediately and
keeps the cart unchanged; the payment deadline also expires that same Checkout
session, so there is no second order-payment timeout. If VNPay reports success
after the Checkout has expired or been canceled, the transaction is logged for
manual refund handling.
Production deployments can set either duration explicitly.

## Required application settings

Set these as environment variables in the runtime environment:

| Environment variable | Purpose |
| --- | --- |
| `CHECKOUT_HOLD_DURATION_MINUTES` | Duration for the inventory hold; defaults to 15 minutes and must be greater than zero. |
| `VNPAY_TIMEOUT_MINUTES` | VNPay payment deadline in minutes; defaults to 15 and must be greater than zero. |
| `VNPAY_QUERY_URL` | VNPay transaction query endpoint; defaults to the sandbox endpoint. Set the production endpoint explicitly when deploying to production. |

The existing `vnpay.tmn-code`, `vnpay.hash-secret`, `vnpay.payment-url`, and
`vnpay.return-url` settings are also required for VNPay. Configure the VNPay
IPN URL in the merchant sandbox to point to `/payment/vnpay-ipn`. Both the IPN
and browser return endpoints verify signed gateway results before completing or
canceling the Checkout.

The destination address is still validated against the configured
administrative location API. Shipping charges are always zero, including when
discount codes are applied.

## Discount policy fields

`discount_codes` supports percentage/fixed discounts, minimum eligible item
subtotal, maximum discount, whether shipping is discountable, and per-customer
use limits. Empty product/category eligibility sets mean the code applies to
all products; otherwise a cart line must match an eligible product or category.
`quantity` is treated as remaining global uses and is decremented atomically
when an order is created. For VNPay it is reserved when payment starts and
restored if the Checkout is canceled or expires before successful payment.

## Orders, carrier callbacks, and returns

Customer order history is available at `/account/orders`; staff order
management and returns are available at `/staff/orders` and `/staff/returns`.
These actions check the current database role and ownership in the service layer.
Order cancellation and status changes serialize on the order row. Cancelling
restores the sold inventory with an audit transaction; a paid order is marked
`REFUND_REQUIRED` and is never treated as refunded automatically. Staff can
record a cancellation refund only after verifying the external transaction;
the reference is saved as an idempotent refund transaction.

Set `RETURN_WINDOW_DAYS` to the agreed common return period. Its default is zero,
which disables new return requests until the business policy is configured.
Return requests are only accepted for delivered orders, and a staff member must
record quantities received and accepted after inspection. The refund amount is
calculated from accepted item snapshots net of the item's allocated discount;
shipping is not refunded. Staff may mark a refund successful only after
verifying the external refund and entering its reference. The system records
that reference and an idempotent refund transaction; it does not claim to call
or verify a gateway refund automatically.

The normalized signed carrier callback endpoint is `/shipping/ghn/webhook`.
Configure `GHN_WEBHOOK_SECRET` with a high-entropy secret shared with the
trusted webhook adapter. The adapter sends JSON fields `eventId`, `orderCode`
(the recorded GHN order code), and `status` (`RECEIVED`, `DELIVERED`, or
`DELIVERY_FAILED`) with `X-GHN-Signature` set to the lowercase hexadecimal
HMAC-SHA256 of the exact request body. The endpoint rejects unsigned, unknown,
or invalid state transitions and stores accepted event IDs for idempotency.
Before handing off a confirmed order, staff must record its GHN order code.
