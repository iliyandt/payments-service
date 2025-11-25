package demos.springdata.paymentservice.service;

import com.stripe.exception.StripeException;
import com.stripe.model.Customer;
import com.stripe.model.checkout.Session;
import com.stripe.param.CustomerCreateParams;
import com.stripe.param.checkout.SessionCreateParams;
import demos.springdata.paymentservice.model.entity.PaymentTenant;
import demos.springdata.paymentservice.repository.PaymentTenantRepository;
import demos.springdata.paymentservice.web.dto.CheckoutRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.Optional;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class SaasStripeServiceUTest {

    @Mock
    private PaymentTenantRepository paymentTenantRepository;

    @Captor
    ArgumentCaptor<PaymentTenant> tenantCaptor;

    @InjectMocks
    private SaasStripeService saasStripeService;

    @Test
    void createSaasCheckoutSession_ShouldCreatePaymentTenant_WhenTenantNotFound() throws StripeException {
        String tenantIdString = UUID.randomUUID().toString();

        CheckoutRequest checkoutRequest = CheckoutRequest.builder()
                .tenantId(tenantIdString)
                .tenantName("TestTenant")
                .businessEmail("test@tenant.bg")
                .currency("BGN")
                .amount(1000L)
                .plan("PRO")
                .abonnementDuration("MONTHLY")
                .build();

        Mockito.when(paymentTenantRepository.findByTenantId(tenantIdString))
                .thenReturn(Optional.empty());

        try(MockedStatic<Customer> customerStatic = mockStatic(Customer.class);
            MockedStatic<Session> sessionStatic = mockStatic(Session.class)) {

            Customer mockCustomer = mock(Customer.class);
            when(mockCustomer.getId()).thenReturn("cus_newly_created_999");

            customerStatic.when(() -> Customer.create(any(CustomerCreateParams.class)))
                    .thenReturn(mockCustomer);

            Session mockSession = mock(Session.class);
            when(mockSession.getId()).thenReturn("cs_test_session_123");

            sessionStatic.when(() -> Session.create(any(SessionCreateParams.class)))
                    .thenReturn(mockSession);

            Session result = saasStripeService.createSaasCheckoutSession(checkoutRequest);

            assertNotNull(result);
            assertEquals("cs_test_session_123", result.getId());

            verify(paymentTenantRepository).save(tenantCaptor.capture());
            PaymentTenant savedTenant = tenantCaptor.getValue();

            assertEquals(tenantIdString, savedTenant.getTenantId());
            assertEquals("test@tenant.bg", savedTenant.getBusinessEmail());
            assertEquals("TestTenant", savedTenant.getName());
        }

    }


    @Test
    void createSaasCheckoutSession_ShouldUseExistingUser_WhenTenantExistsAndHasStripeId() throws StripeException {
        String tenantIdString = UUID.randomUUID().toString();
        CheckoutRequest request = CheckoutRequest.builder()
                .tenantId(tenantIdString)
                .businessEmail("existing@gym.bg")
                .tenantName("Existing Gym")
                .currency("USD")
                .amount(2000L)
                .plan("PRO")
                .abonnementDuration("ANNUALLY")
                .build();


        PaymentTenant paymentTenant = PaymentTenant.builder()
                .id(UUID.randomUUID())
                .tenantId(tenantIdString)
                .name(request.getTenantName())
                .businessEmail(request.getBusinessEmail())
                .stripeCustomerId("cus_existing_123")
                .build();

        Mockito.when(paymentTenantRepository.findByTenantId(tenantIdString))
                .thenReturn(Optional.of(paymentTenant));


        try (MockedStatic<Customer> customerStatic = mockStatic(Customer.class);
             MockedStatic<Session> sessionStatic = mockStatic(Session.class)) {

            Session mockSession = mock(Session.class);
            when(mockSession.getId()).thenReturn("cs_session_existing_user");

            sessionStatic.when(() -> Session.create(any(SessionCreateParams.class)))
                    .thenReturn(mockSession);

            Session result = saasStripeService.createSaasCheckoutSession(request);

            assertEquals("cs_session_existing_user", result.getId());

            customerStatic.verify(() -> Customer.create(any(CustomerCreateParams.class)), never());

            verify(paymentTenantRepository).save(tenantCaptor.capture());
            PaymentTenant updatedTenant = tenantCaptor.getValue();

            assertEquals("existing@gym.bg", updatedTenant.getBusinessEmail());
            assertEquals("cus_existing_123", updatedTenant.getStripeCustomerId());
        }
    }
}
