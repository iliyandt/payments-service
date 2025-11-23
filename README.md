## Stripe Integration & Local Development Limitations

The project utilizes **Stripe Webhooks** for subscription activation. Since webhooks require a local Stripe CLI tunnel authenticated with the repository owner's account, the end-to-end payment flow **cannot be tested locally** by external users without access to the specific Stripe Dashboard.

### Workaround for Local Testing (Debug Mode)
To facilitate evaluation without Stripe access, a **bypass endpoint** is exposed. This endpoint simulates a successful webhook event and activates the subscription locally.

**Endpoint:** `POST /api/v1/debug/simulate-success/saas`

**How to use:**
1. Register a new user/tenant locally.
2. Check the database or UI to find your `tenantId`.
3. Execute the following cURL command (or use Postman):

```bash
 # Replace '170' with your actual tenantId
curl -X POST "http://localhost:8081/api/v1/debug/simulate-success/saas?tenantId=170&planName=PRO&duration=MONTHLY"
```

# **Live Demo**

The full payment flow (including real Stripe Webhooks) is fully functional on the production deployment: 👉 https://damilsoft.com/

**Test Card Credentials:**

Card Number: 4242 4242 4242 4242
Date: Any future date (e.g., 12/30)
CVC: Any 3 digits (e.g., 123)