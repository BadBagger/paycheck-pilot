# Paycheck Pilot Bank Sync Backend

This backend follows the same security pattern as Renewal Radar:

- Android uses Plaid Link only.
- Android receives a `public_token` from Plaid Link and sends it here.
- This backend exchanges the `public_token` for a Plaid `access_token`.
- Plaid access tokens are encrypted at rest and never returned to Android.
- Android receives only safe connected-account, paycheck, bill, and spending summaries.

## Endpoints

- `GET /health`
- `POST /api/plaid/create-link-token`
- `POST /api/plaid/exchange-public-token`
- `POST /api/plaid/sync-transactions`
- `POST /api/paycheck/sync`
- `POST /api/plaid/disconnect`
- `GET /api/account/export`
- `POST /api/account/delete`

Use `PLAID_MOCK_MODE=true` for local/demo mode. Real Plaid requires a hosted HTTPS backend with Plaid credentials in environment variables.
