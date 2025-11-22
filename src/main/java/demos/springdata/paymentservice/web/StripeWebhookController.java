package demos.springdata.paymentservice.web;

import com.stripe.exception.SignatureVerificationException;
import com.stripe.model.Event;
import com.stripe.model.EventDataObjectDeserializer;
import com.stripe.model.StripeObject;
import com.stripe.model.checkout.Session;
import com.stripe.net.Webhook;
import demos.springdata.paymentservice.client.MonolithFeignClient;
import demos.springdata.paymentservice.config.StripeProperties;
import demos.springdata.paymentservice.web.dto.SubscriptionRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/stripe/webhook")
public class StripeWebhookController {

    private final MonolithFeignClient monolithClient;
    private final StripeProperties properties;
    private static final Logger LOGGER = LoggerFactory.getLogger(StripeWebhookController.class);


    @Autowired
    public StripeWebhookController(MonolithFeignClient monolithClient, StripeProperties properties) {
        this.monolithClient = monolithClient;
        this.properties = properties;
    }

    @PostMapping
    public ResponseEntity<String> handleWebhook(@RequestBody String payload, @RequestHeader(name = "Stripe-Signature", required = false) String signatureHeader) {
        if (signatureHeader == null || properties.getWebhookSecret() == null) {
            LOGGER.warn("Missing webhook signature/secret");
            return ResponseEntity.badRequest().body("Missing webhook signature/secret");
        }

        final Event event;
        try {
            event = Webhook.constructEvent(payload, signatureHeader, properties.getWebhookSecret());
        } catch (SignatureVerificationException ex) {
            LOGGER.warn("Invalid Stripe signature", ex);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Invalid signature");
        }

        EventDataObjectDeserializer dataObjectDeserializer = event.getDataObjectDeserializer();
        StripeObject stripeObject = dataObjectDeserializer.getObject().orElse(null);
        if (stripeObject == null) {
            LOGGER.error("Failed to deserialize event data. Event ID: {}", event.getId());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Deserialization failed");
        }

        try {
            switch (event.getType()) {
                case "checkout.session.completed" -> {
                    Session session = (Session) stripeObject;
                    String type = session.getMetadata().get("type");

                    if ("SAAS_SUBSCRIPTION".equals(type)) {
                        monolithClient.activateTenantSubscription(
                                session.getMetadata().get("tenantId"),
                                session.getMetadata().get("planName"),
                                session.getMetadata().get("abonnementDuration")
                        );
                    } else if ("GYM_MEMBERSHIP".equals(type)) {

                        SubscriptionRequest request = new SubscriptionRequest
                                (
                                        Integer.valueOf(session.getMetadata().get("allowedVisits")),
                                        session.getMetadata().get("subscriptionPlan"),
                                        session.getMetadata().get("employment")
                                );

                        monolithClient.activateUserMembership
                                (session.getMetadata().get("userId"), request);
                    } else {
                        LOGGER.info("Unknown checkout type: {}", type);
                    }
                }
                default -> LOGGER.info("Unhandled event type: {}", event.getType());
            }

            return ResponseEntity.ok("received");
        } catch (Exception ex) {
            LOGGER.error("Error while handling event {}", event.getId(), ex);
            return ResponseEntity.ok("Error");
        }
    }
}
