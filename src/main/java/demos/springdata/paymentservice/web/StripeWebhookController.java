package demos.springdata.paymentservice.web;

import com.stripe.exception.SignatureVerificationException;
import com.stripe.model.Event;
import com.stripe.model.EventDataObjectDeserializer;
import com.stripe.model.StripeObject;
import com.stripe.model.checkout.Session;
import com.stripe.net.Webhook;
import demos.springdata.paymentservice.client.MonolithFeignClient;
import demos.springdata.paymentservice.config.StripeProperties;
import demos.springdata.paymentservice.model.entity.PaymentTenant;
import demos.springdata.paymentservice.model.enums.SubscriptionStatus;
import demos.springdata.paymentservice.repository.PaymentTenantRepository;
import demos.springdata.paymentservice.service.SaasStripeService;
import demos.springdata.paymentservice.service.StripeWebhookService;
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

    private final StripeProperties properties;
    private final StripeWebhookService stripeWebhookService;
    private static final Logger LOGGER = LoggerFactory.getLogger(StripeWebhookController.class);


    @Autowired
    public StripeWebhookController(StripeProperties properties, StripeWebhookService stripeWebhookService) {
        this.properties = properties;
        this.stripeWebhookService = stripeWebhookService;
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

        try {
            stripeWebhookService.handleEvent(event);
            return ResponseEntity.ok("received");
        } catch (Exception ex) {
            LOGGER.error("Error while handling event {}", event.getId(), ex);
            return ResponseEntity.ok("Error");
        }
    }
}
