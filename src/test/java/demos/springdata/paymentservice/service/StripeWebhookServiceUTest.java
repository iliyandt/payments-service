package demos.springdata.paymentservice.service;

import com.stripe.model.Event;
import com.stripe.model.EventDataObjectDeserializer;
import com.stripe.model.checkout.Session;
import demos.springdata.paymentservice.client.MonolithFeignClient;
import demos.springdata.paymentservice.model.entity.PaymentCustomer;
import demos.springdata.paymentservice.model.entity.PaymentTenant;
import demos.springdata.paymentservice.model.enums.SubscriptionStatus;
import demos.springdata.paymentservice.repository.PaymentCustomerRepository;
import demos.springdata.paymentservice.repository.PaymentTenantRepository;
import demos.springdata.paymentservice.web.dto.SubscriptionRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class StripeWebhookServiceUTest {

    @Mock
    private MonolithFeignClient monolithClient;
    @Mock
    private PaymentTenantRepository paymentTenantRepository;
    @Mock
    private PaymentCustomerRepository paymentCustomerRepository;

    @Captor
    private ArgumentCaptor<PaymentTenant> tenantCaptor;

    @Captor
    private ArgumentCaptor<PaymentCustomer> customerCaptor;

    @Captor
    private ArgumentCaptor<SubscriptionRequest> subscriptionRequestCaptor;

    @InjectMocks
    private StripeWebhookService webhookService;


    @Test
    void handleEvent_ShouldActivateSaasSubscription_WhenTypeIsSaas() {
        String tenantId = UUID.randomUUID().toString();
        String planName = "GROWTH";
        String duration = "YEARLY";
        String stripeSubId = "sub_123456";

        Map<String, String> metadata = new HashMap<>();
        metadata.put("type", "SAAS_SUBSCRIPTION");
        metadata.put("tenantId", tenantId);
        metadata.put("planName", planName);
        metadata.put("abonnementDuration", duration);

        Event event = mock(Event.class);
        EventDataObjectDeserializer deserializer = mock(EventDataObjectDeserializer.class);
        Session session = mock(Session.class);

        when(event.getType()).thenReturn("checkout.session.completed");
        when(event.getDataObjectDeserializer()).thenReturn(deserializer);
        when(deserializer.getObject()).thenReturn(Optional.of(session));

        when(session.getMetadata()).thenReturn(metadata);
        when(session.getSubscription()).thenReturn(stripeSubId);

        PaymentTenant existingTenant = PaymentTenant.builder()
                .tenantId(tenantId)
                .status(SubscriptionStatus.INACTIVE)
                .build();

        when(paymentTenantRepository.findByTenantId(tenantId)).thenReturn(Optional.of(existingTenant));

        webhookService.handleEvent(event);

        Mockito.verify(paymentTenantRepository).save(tenantCaptor.capture());
        PaymentTenant savedTenant = tenantCaptor.getValue();
        assertEquals(SubscriptionStatus.ACTIVE, savedTenant.getStatus());
        assertEquals(planName, savedTenant.getCurrentPlanName());
        assertEquals(duration, savedTenant.getBillingPeriod());
        assertEquals(stripeSubId, savedTenant.getStripeSubscriptionId());

        Mockito.verify(monolithClient).activateTenantSubscription(tenantId, planName, duration);
    }

    @Test
    void handleEvent_ShouldActivateGymMembership_WhenTypeIsGymMembership() {

        String userId = UUID.randomUUID().toString();
        String stripeCustomerId = "cus_gym_user_789";
        String plan = "PRO";
        String employment = "MONTHLY";

        Map<String, String> metadata = new HashMap<>();
        metadata.put("type", "GYM_MEMBERSHIP");
        metadata.put("userId", userId);
        metadata.put("subscriptionPlan", plan);
        metadata.put("employment", employment);

        Event event = mock(Event.class);
        EventDataObjectDeserializer deserializer = mock(EventDataObjectDeserializer.class);

        Session session = new Session();
        session.setMetadata(metadata);
        session.setCustomer(stripeCustomerId);
        session.setSubscription("sub_gym_456");
        session.setId("cs_test_gym_123");

        when(event.getType()).thenReturn("checkout.session.completed");
        when(event.getDataObjectDeserializer()).thenReturn(deserializer);

        when(deserializer.getObject()).thenReturn(Optional.of(session));

        when(paymentCustomerRepository.findByUserId(userId)).thenReturn(Optional.empty());

        webhookService.handleEvent(event);

        verify(paymentCustomerRepository).save(customerCaptor.capture());
        PaymentCustomer savedCustomer = customerCaptor.getValue();
        assertEquals(userId, savedCustomer.getUserId());
        assertEquals(stripeCustomerId, savedCustomer.getStripeCustomerId());

        verify(monolithClient).activateUserMembership(eq(userId), subscriptionRequestCaptor.capture());
        SubscriptionRequest request = subscriptionRequestCaptor.getValue();
        assertEquals(plan, request.subscriptionPlan());
        assertEquals(employment, request.employment());
    }

    @Test
    void handleEvent_ShouldHandleDeserializationError_Gracefully() {

        Event event = mock(Event.class);
        EventDataObjectDeserializer deserializer = mock(EventDataObjectDeserializer.class);

        when(event.getDataObjectDeserializer()).thenReturn(deserializer);

        when(deserializer.getObject()).thenReturn(Optional.empty());
        when(event.getId()).thenReturn("evt_error_123");

        webhookService.handleEvent(event);

        verifyNoInteractions(monolithClient);
    }
}
