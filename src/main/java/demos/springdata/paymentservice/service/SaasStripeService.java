package demos.springdata.paymentservice.service;

import com.stripe.exception.StripeException;
import com.stripe.model.Customer;
import com.stripe.model.checkout.Session;
import com.stripe.param.CustomerCreateParams;
import com.stripe.param.checkout.SessionCreateParams;
import demos.springdata.paymentservice.model.entity.PaymentTenant;
import demos.springdata.paymentservice.model.enums.SubscriptionStatus;
import demos.springdata.paymentservice.repository.PaymentTenantRepository;
import demos.springdata.paymentservice.web.dto.CheckoutRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class SaasStripeService {

    private final PaymentTenantRepository paymentTenantRepository;

    @Autowired
    public SaasStripeService(PaymentTenantRepository paymentTenantRepository) {
        this.paymentTenantRepository = paymentTenantRepository;
    }


    public Session createSaasCheckoutSession(CheckoutRequest request) throws StripeException {

        PaymentTenant tenant = paymentTenantRepository.findByTenantId(request.getTenantId())
                .orElseGet(() -> PaymentTenant.builder()
                        .tenantId(request.getTenantId())
                        .status(SubscriptionStatus.INACTIVE)
                        .build());

        tenant.setBusinessEmail(request.getBusinessEmail());
        tenant.setName(request.getTenantName());

        String customerId = tenant.getStripeCustomerId();

        if (customerId == null) {
            Customer customer = Customer.create(CustomerCreateParams.builder()
                    .setEmail(request.getBusinessEmail())
                    .setName(request.getTenantName())
                    .putMetadata("tenantId", String.valueOf(request.getTenantId()))
                    .build());

            customerId = customer.getId();
            tenant.setStripeCustomerId(customerId);
        }

        paymentTenantRepository.save(tenant);


        SessionCreateParams.Builder params = SessionCreateParams.builder()
                .setCustomer(customerId)
                .setMode(SessionCreateParams.Mode.PAYMENT)
                .setUiMode(SessionCreateParams.UiMode.HOSTED)
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
                                                                .setName(request.getPlan() + " - " + request.getAbonnementDuration())
                                                                .build()
                                                )
                                                .build()
                                )
                                .build()
                )
                .putMetadata("type", "SAAS_SUBSCRIPTION")
                .putMetadata("tenantId", request.getTenantId())
                .putMetadata("planName", request.getPlan())
                .putMetadata("abonnementDuration", request.getAbonnementDuration());

        return Session.create(params.build());
    }
}
