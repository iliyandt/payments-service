package demos.springdata.paymentservice.service;

import com.stripe.exception.StripeException;
import com.stripe.model.Account;
import com.stripe.model.AccountLink;
import com.stripe.model.Customer;
import com.stripe.model.checkout.Session;
import com.stripe.net.RequestOptions;
import com.stripe.param.AccountCreateParams;
import com.stripe.param.AccountLinkCreateParams;
import com.stripe.param.CustomerCreateParams;
import com.stripe.param.checkout.SessionCreateParams;
import demos.springdata.paymentservice.exception.PaymentException;
import demos.springdata.paymentservice.model.entity.PaymentCustomer;
import demos.springdata.paymentservice.model.entity.StripeConnectAccount;
import demos.springdata.paymentservice.repository.ConnectRepository;
import demos.springdata.paymentservice.repository.PaymentCustomerRepository;
import demos.springdata.paymentservice.web.dto.AccountLinkResponse;
import demos.springdata.paymentservice.web.dto.ConnectedCheckoutRequest;
import demos.springdata.paymentservice.web.dto.TenantDto;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class ConnectStripeServiceUTest {

    @Mock
    private ConnectRepository connectRepository;
    @Mock
    private PaymentCustomerRepository paymentCustomerRepository;
    @Captor
    private ArgumentCaptor<StripeConnectAccount> captor;

    @Captor
    private ArgumentCaptor<PaymentCustomer> customerCaptor;

    @Captor
    private ArgumentCaptor<SessionCreateParams> sessionParamsCaptor;

    @InjectMocks
    private ConnectStripeService connectStripeService;


    @Test
    void createConnectedAccount_ShouldReturnExistingAccount_WhenTenantAlreadyHasOne() throws StripeException {
        UUID tenantId = UUID.randomUUID();
        TenantDto tenantDto = TenantDto.builder()
                .id(tenantId)
                .name("Test Gym")
                .businessEmail("test@gym.bg")
                .build();

        StripeConnectAccount existingEntity = new StripeConnectAccount();
        existingEntity.setStripeAccountId("acct_existing123");


        Mockito.when(connectRepository.findByTenantId(tenantId.toString()))
                .thenReturn(Optional.of(existingEntity));

        try (MockedStatic<Account> mockedAccountStatic = mockStatic(Account.class)){
            Account mockStripeAccount = mock(Account.class);
            Mockito.when(mockStripeAccount.getId()).thenReturn("acct_existing123");

            mockedAccountStatic.when(() -> Account.retrieve("acct_existing123"))
                    .thenReturn(mockStripeAccount);

            Account result = connectStripeService.createConnectedAccount(tenantDto);

            Assertions.assertEquals("acct_existing123", result.getId());
            Mockito.verify(connectRepository, never()).save(any());
        }
    }

    @Test
    void createConnectedAccount_ShouldReturnNewAccount_WhenTenantDoNotHaveOne() throws StripeException{
        UUID tenantId = UUID.randomUUID();
        TenantDto tenantDto = TenantDto.builder()
                .id(tenantId)
                .name("Test Gym")
                .businessEmail("test@gym.bg")
                .build();

        Mockito.when(connectRepository.findByTenantId(tenantId.toString()))
                .thenReturn(Optional.empty());


        try(MockedStatic<Account> mockedAccountStatic = mockStatic(Account.class)) {
            Account mockStripeAccount = mock(Account.class);
            Mockito.when(mockStripeAccount.getId()).thenReturn("acct_newCreated123");

            mockedAccountStatic.when(() -> Account.create(any(AccountCreateParams.class)))
                    .thenReturn(mockStripeAccount);

            Account result = connectStripeService.createConnectedAccount(tenantDto);

            assertEquals("acct_newCreated123", result.getId());

            verify(connectRepository).save(captor.capture());

            StripeConnectAccount savedEntity = captor.getValue();
            assertEquals(tenantId.toString(), savedEntity.getTenantId());
            assertEquals("acct_newCreated123", savedEntity.getStripeAccountId());
        }

    }


    @Test
    void createAccountLink_ShouldThrow_WhenTenantDoNotHaveStripeAccountId() {
        String stripeAccountId = "acct_nonExisting123";
        String returnUrl = "https://damilsoft.com/return";
        String refreshUrl = "https://damilsoft.com/refresh";

        Mockito.when(connectRepository.findByStripeAccountId(stripeAccountId))
                .thenReturn(Optional.empty());

        PaymentException exception = assertThrows(PaymentException.class, () -> {
            connectStripeService.createAccountLink(stripeAccountId, returnUrl, refreshUrl);
        });

        assertEquals("Tenant not connected to Stripe", exception.getMessage());
    }

    @Test
    void createAccountLink_ShouldReturnLink_WhenStripeAccountIdForTenantFound() throws StripeException {
        String stripeAccountId = "acct_nonExisting123";
        String returnUrl = "https://damilsoft.com/return";
        String refreshUrl = "https://damilsoft.com/refresh";

        StripeConnectAccount existingEntity = new StripeConnectAccount();
        existingEntity.setStripeAccountId("acct_existing123");

        Mockito.when(connectRepository.findByStripeAccountId(stripeAccountId))
                .thenReturn(Optional.of(existingEntity));


        try(MockedStatic<Account> mockedAccountStatic = mockStatic(Account.class);
            MockedStatic<AccountLink> mockedLinkStatic = mockStatic(AccountLink.class)) {

            Account mockStripeAccount = mock(Account.class);
            mockedAccountStatic.when(() -> Account.retrieve(existingEntity.getStripeAccountId()))
                    .thenReturn(mockStripeAccount);

            AccountLink mockAccountLink = mock(AccountLink.class);
            String expectedUrl = "https://connect.stripe.com/setup/s/something";
            long createdTime = 1000L;
            long expiresAtTime = 2000L;

            when(mockAccountLink.getUrl()).thenReturn(expectedUrl);
            when(mockAccountLink.getCreated()).thenReturn(createdTime);
            when(mockAccountLink.getExpiresAt()).thenReturn(expiresAtTime);

            mockedLinkStatic.when(() -> AccountLink.create(any(AccountLinkCreateParams.class)))
                    .thenReturn(mockAccountLink);

            AccountLinkResponse result = connectStripeService.createAccountLink(stripeAccountId, returnUrl, refreshUrl);

            assertNotNull(result);
            assertEquals(expectedUrl, result.getUrl());
            assertEquals(createdTime, result.getCreated());
            assertEquals(expiresAtTime, result.getExpiresAt());

        }
    }


    @Test
    void createMemberCheckoutSession_ShouldCreateCustomerAndSession_WhenCustomerDoesNotExist() throws StripeException {

        String stripeAccountId = "acct_gym_123";
        UUID userId = UUID.randomUUID();
        String userEmail = "trainee@gym.bg";

        ConnectedCheckoutRequest request = new ConnectedCheckoutRequest();
        request.setUserId(userId);
        request.setEmail(userEmail);
        request.setName("Ivan Trainee");
        request.setCurrency("BGN");
        request.setAmount(5000L);
        request.setSubscriptionPlan("PRO");
        request.setEmployment("MONTHLY");
        request.setAllowedVisits(12);

        StripeConnectAccount connectAccount = new StripeConnectAccount();
        connectAccount.setStripeAccountId(stripeAccountId);

        when(connectRepository.findByStripeAccountId(stripeAccountId))
                .thenReturn(Optional.of(connectAccount));

        when(paymentCustomerRepository.findByUserIdAndStripeConnectedAccountId(eq(userId.toString()), eq(stripeAccountId)))
                .thenReturn(Optional.empty());

        try (MockedStatic<Session> sessionStatic = mockStatic(Session.class);
             MockedStatic<Customer> customerStatic = mockStatic(Customer.class)) {

            Customer mockCustomer = mock(Customer.class);
            when(mockCustomer.getId()).thenReturn("cus_newly_created_123");

            customerStatic.when(() -> Customer.create(any(CustomerCreateParams.class), any(RequestOptions.class)))
                    .thenReturn(mockCustomer);

            Session mockSession = mock(Session.class);
            when(mockSession.getId()).thenReturn("cs_test_session_xyz");

            sessionStatic.when(() -> Session.create(any(SessionCreateParams.class), any(RequestOptions.class)))
                    .thenReturn(mockSession);

            Session result = connectStripeService.createMemberCheckoutSession(stripeAccountId, request);

            assertNotNull(result);
            assertEquals("cs_test_session_xyz", result.getId());

            verify(paymentCustomerRepository).save(customerCaptor.capture());
            PaymentCustomer savedCustomer = customerCaptor.getValue();
            assertEquals(userId.toString(), savedCustomer.getUserId());
            assertEquals("cus_newly_created_123", savedCustomer.getStripeCustomerId());
            assertEquals(stripeAccountId, savedCustomer.getStripeConnectedAccountId());

            sessionStatic.verify(() -> Session.create(sessionParamsCaptor.capture(), any(RequestOptions.class)));
            SessionCreateParams capturedParams = sessionParamsCaptor.getValue();

            assertEquals("cus_newly_created_123", capturedParams.getCustomer());


            assertEquals("GYM_MEMBERSHIP", capturedParams.getMetadata().get("type"));
            assertEquals(userId.toString(), capturedParams.getMetadata().get("userId"));


            assertEquals(1L, capturedParams.getLineItems().get(0).getQuantity());
            assertEquals(5000L, capturedParams.getLineItems().get(0).getPriceData().getUnitAmount());
        }
    }
}
