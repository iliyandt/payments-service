package demos.springdata.paymentservice.web;

import com.stripe.exception.StripeException;
import com.stripe.model.Account;
import com.stripe.model.checkout.Session;
import demos.springdata.paymentservice.service.ConnectStripeService;
import demos.springdata.paymentservice.service.SaasStripeService;
import demos.springdata.paymentservice.web.dto.AccountLinkResponse;
import demos.springdata.paymentservice.web.dto.CheckoutRequest;
import demos.springdata.paymentservice.web.dto.ConnectedCheckoutRequest;
import demos.springdata.paymentservice.web.dto.TenantDto;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/payments")
public class PaymentController {
    private final SaasStripeService saasService;
    private final ConnectStripeService connectService;

    public PaymentController(SaasStripeService saasService, ConnectStripeService connectService) {
        this.saasService = saasService;
        this.connectService = connectService;
    }

    @PostMapping("/saas/checkout")
    public ResponseEntity<String> createSaasCheckoutSession(@RequestBody CheckoutRequest request) throws StripeException {
        Session session = saasService.createSaasCheckoutSession(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(session.getUrl());
    }

    @PostMapping("/connect/checkout")
    public ResponseEntity<String> createMemberCheckoutSession(
            @RequestParam("connectedAccountId") String connectedAccountId,
            @RequestBody ConnectedCheckoutRequest request) throws StripeException {

        Session session = connectService.createMemberCheckoutSession(connectedAccountId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(session.getUrl());
    }

    @PostMapping("/connect/onboard")
    public ResponseEntity<AccountLinkResponse> createAccountLink(
            @RequestParam("connectedAccountId") String connectedAccountId,
            @RequestParam("returnUrl") String returnUrl,
            @RequestParam("refreshUrl") String refreshUrl) throws StripeException {

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(connectService.createAccountLink(connectedAccountId, returnUrl, refreshUrl));
    }

    @PostMapping("/connect/create-account")
    public ResponseEntity<String> createConnectedAccount(@RequestBody TenantDto tenant) throws StripeException {
        Account connectedAccount = connectService.createConnectedAccount(tenant);
        return ResponseEntity.status(HttpStatus.CREATED).body(connectedAccount.getId());
    }
}
