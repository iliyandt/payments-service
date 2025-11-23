package demos.springdata.paymentservice.service;

import com.stripe.exception.StripeException;
import com.stripe.model.Account;
import com.stripe.model.AccountLink;
import com.stripe.model.checkout.Session;
import com.stripe.net.RequestOptions;
import com.stripe.param.AccountCreateParams;
import com.stripe.param.AccountLinkCreateParams;
import com.stripe.param.checkout.SessionCreateParams;
import demos.springdata.paymentservice.model.entity.StripeConnectAccount;
import demos.springdata.paymentservice.repository.ConnectRepository;
import demos.springdata.paymentservice.web.dto.AccountLinkResponse;
import demos.springdata.paymentservice.web.dto.ConnectedCheckoutRequest;
import demos.springdata.paymentservice.web.dto.TenantDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class ConnectStripeService {

    private final ConnectRepository connectRepository;
    private static final Logger LOGGER = LoggerFactory.getLogger(ConnectStripeService.class);

    @Autowired
    public ConnectStripeService(ConnectRepository connectRepository) {
        this.connectRepository = connectRepository;
    }

    public Account createConnectedAccount(TenantDto tenant) throws StripeException {

        String tenantIdString = tenant.getId().toString();

        Optional<StripeConnectAccount> existingEntry = connectRepository.findByTenantId(tenantIdString);

        if (existingEntry.isPresent()) {
            String existingStripeId = existingEntry.get().getStripeAccountId();
            LOGGER.info("Tenant with ID {} already has a Stripe account: {}", tenantIdString, existingStripeId);

            return Account.retrieve(existingStripeId);
        }

        AccountCreateParams.Capabilities capabilities =
                AccountCreateParams.Capabilities.builder()
                        .setCardPayments(
                                AccountCreateParams.Capabilities.CardPayments.builder().setRequested(true).build()
                        )
                        .setTransfers(
                                AccountCreateParams.Capabilities.Transfers.builder().setRequested(true).build()
                        )
                        .build();

        AccountCreateParams params =
                AccountCreateParams.builder()
                        .setType(AccountCreateParams.Type.EXPRESS)
                        .setCountry("BG")
                        .setEmail(tenant.getBusinessEmail())
                        .setCapabilities(capabilities)
                        .setBusinessType(AccountCreateParams.BusinessType.COMPANY)
                        .setBusinessProfile(
                                AccountCreateParams.BusinessProfile
                                        .builder()
                                        .setName(tenant.getName())
                                        .setProductDescription("Subscription")
                                        .setMcc("7941")
                                        .build()
                        )
                        .build();

        Account account = Account.create(params);

        StripeConnectAccount entity = new StripeConnectAccount();
        entity.setTenantId(tenant.getId().toString());
        entity.setStripeAccountId(account.getId());
        connectRepository.save(entity);

        return account;
    }

    public AccountLinkResponse createAccountLink(String tenantId, String returnUrl, String refreshUrl) throws StripeException {

        StripeConnectAccount connectAccount = connectRepository.findByStripeAccountId(tenantId)
                .orElseThrow(() -> new RuntimeException("Tenant not connected to Stripe"));

        AccountLinkCreateParams params =
                AccountLinkCreateParams.builder()
                        .setAccount(connectAccount.getStripeAccountId())
                        .setRefreshUrl(refreshUrl)
                        .setReturnUrl(returnUrl)
                        .setType(AccountLinkCreateParams.Type.ACCOUNT_ONBOARDING)
                        .setCollect(AccountLinkCreateParams.Collect.EVENTUALLY_DUE)
                        .build();

        Account account = Account.retrieve(connectAccount.getStripeAccountId());

        LOGGER.info("Account email: {}", account.getEmail());

        AccountLink accountLink = AccountLink.create(params);

        LOGGER.info("Account link info: {}", accountLink);

        return AccountLinkResponse.builder()
                .url(accountLink.getUrl())
                .created(accountLink.getCreated())
                .expiresAt(accountLink.getExpiresAt())
                .build();
    }


    public Session createMemberCheckoutSession(String stripeAccountId, ConnectedCheckoutRequest request) throws StripeException {

        StripeConnectAccount connectAccount = connectRepository.findByStripeAccountId(stripeAccountId)
                .orElseThrow(() -> new RuntimeException("Tenant not connected to Stripe"));

        SessionCreateParams params =
                SessionCreateParams.builder()
                        .setSuccessUrl("https://damilsoft.com/success?session_id={CHECKOUT_SESSION_ID}")
                        .setCancelUrl("https://damilsoft.com/cancel")
                        .addLineItem(
                                SessionCreateParams.LineItem.builder()
                                        .setQuantity(1L)
                                        .setPriceData(
                                                SessionCreateParams.LineItem.PriceData.builder()
                                                        .setCurrency(request.getCurrency())
                                                        .setUnitAmount(request.getAmount())
                                                        .setProductData(
                                                                SessionCreateParams.LineItem.PriceData.ProductData.builder()
                                                                        .setName(request.getSubscriptionPlan() + " - " + request.getEmployment())
                                                                        .build()
                                                        )
                                                        .build()
                                        )
                                        .build()
                        )
                        .setMode(SessionCreateParams.Mode.PAYMENT)
                        .putMetadata("type", "GYM_MEMBERSHIP")
                        .putMetadata("userId", request.getUserId().toString())
                        .putMetadata("subscriptionPlan", request.getSubscriptionPlan())
                        .putMetadata("allowedVisits", String.valueOf(request.getAllowedVisits()))
                        .putMetadata("employment", request.getEmployment())
                        .build();


        RequestOptions options = RequestOptions.builder()
                .setStripeAccount(connectAccount.getStripeAccountId())
                .build();

        return Session.create(params, options);

    }



}
