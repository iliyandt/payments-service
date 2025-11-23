package demos.springdata.paymentservice.service;

import com.stripe.model.Event;
import com.stripe.model.EventDataObjectDeserializer;
import com.stripe.model.StripeObject;
import com.stripe.model.checkout.Session;
import demos.springdata.paymentservice.client.MonolithFeignClient;
import demos.springdata.paymentservice.model.entity.PaymentTenant;
import demos.springdata.paymentservice.model.enums.SubscriptionStatus;
import demos.springdata.paymentservice.repository.PaymentTenantRepository;
import demos.springdata.paymentservice.web.dto.SubscriptionRequest;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class StripeWebhookService {

    private static final Logger LOGGER = LoggerFactory.getLogger(StripeWebhookService.class);

    private final MonolithFeignClient monolithClient;
    private final PaymentTenantRepository paymentTenantRepository;

    @Autowired
    public StripeWebhookService(MonolithFeignClient monolithClient, PaymentTenantRepository paymentTenantRepository) {
        this.monolithClient = monolithClient;
        this.paymentTenantRepository = paymentTenantRepository;
    }

    @Transactional
    public void handleEvent(Event event) {
        EventDataObjectDeserializer dataObjectDeserializer = event.getDataObjectDeserializer();
        StripeObject stripeObject = dataObjectDeserializer.getObject().orElse(null);

        if (stripeObject == null) {
            LOGGER.error("Failed to deserialize event data. Event ID: {}", event.getId());
            return;
        }

        if (event.getType().equals("checkout.session.completed")) {
            handleCheckoutSessionCompleted((Session) stripeObject);
        } else {
            LOGGER.info("Unhandled event type: {}", event.getType());
        }
    }


    public void handleCheckoutSessionCompleted(Session session) {

        String type = session.getMetadata().get("type");

        if ("SAAS_SUBSCRIPTION".equals(type)) {

            updateLocalTenantSubscription(session);

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


    private void updateLocalTenantSubscription(Session session) {
        String tenantId = session.getMetadata().get("tenantId");
        String planName = session.getMetadata().get("planName");
        String duration = session.getMetadata().get("abonnementDuration");


        PaymentTenant tenant = paymentTenantRepository.findByTenantId(tenantId)
                .orElseThrow(() -> new RuntimeException("Tenant not found for ID: " + tenantId));

        tenant.setStatus(SubscriptionStatus.ACTIVE);
        tenant.setCurrentPlanName(planName);
        tenant.setBillingPeriod(duration);

        if (session.getSubscription() != null) {
            tenant.setStripeSubscriptionId(session.getSubscription());
        }

        paymentTenantRepository.save(tenant);

        LOGGER.info("Updated PaymentTenant {} to ACTIVE status with plan {}", tenantId, planName);
    }
}

