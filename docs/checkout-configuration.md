# Checkout configuration

Checkout deliberately fails closed until external shipping/payment settings and
inventory packaging data are configured. It never substitutes a zero shipping
fee or an invented inventory/payment deadline.

## Required application settings

Set these as environment variables in the runtime environment:

| Environment variable | Purpose |
| --- | --- |
| `CHECKOUT_HOLD_DURATION_MINUTES` | Duration for the inventory hold; must be greater than zero. |
| `GHN_API_BASE_URL` | GHN API base URL for the selected environment. |
| `GHN_API_TOKEN` | GHN API token. |
| `GHN_SHOP_ID` | GHN shop identifier. |
| `GHN_SERVICE_TYPE_ID` | Supported GHN service type. |
| `VNPAY_TIMEOUT_MINUTES` | VNPay payment deadline; required for VNPay orders. |
| `VNPAY_QUERY_URL` | VNPay transaction query endpoint, required to verify payment state before releasing expired orders. |

The existing `vnpay.tmn-code`, `vnpay.hash-secret`, `vnpay.payment-url`, and
`vnpay.return-url` settings are also required for VNPay. Configure the VNPay
IPN URL in the merchant sandbox to point to `/payment/vnpay-ipn`. The browser
return endpoint only displays the order's backend payment status; it does not
confirm payment.

## Branch and product shipping data

The schema adds `branches.ghn_district_id` and `branches.ghn_ward_code`.
Populate both for each shipping branch using the corresponding GHN location
codes. Each purchasable product variant needs positive packed-unit values for
`shipping_weight_grams`, `shipping_length_cm`, `shipping_width_cm`, and
`shipping_height_cm`. The quote uses total item weight, the maximum item length
and width, and the sum of packed-unit heights (a conservative vertical-stack
packing assumption). GHN quoting is rejected if any required value is absent.

The destination address is validated against the configured administrative
location API and its district/ward codes are sent to GHN. Shipping is quoted
again immediately before order creation. If GHN fails or the quote changes,
Checkout cannot finalize until the customer reviews a valid quote.

## Discount policy fields

`discount_codes` supports percentage/fixed discounts, minimum eligible item
subtotal, maximum discount, whether shipping is discountable, and per-customer
use limits. Empty product/category eligibility sets mean the code applies to
all products; otherwise a cart line must match an eligible product or category.
`quantity` is treated as remaining global uses and is decremented atomically
when an order is created. A use is not restored on cancellation because that
policy has not been specified.

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
