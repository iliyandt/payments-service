package demos.springdata.paymentservice.web;

import com.stripe.exception.StripeException;
import demos.springdata.paymentservice.client.MonolithFeignClient;
import demos.springdata.paymentservice.web.dto.SubscriptionRequest;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/debug")
@Profile("dev")
public class DebugPaymentController {

    private final MonolithFeignClient monolithFeignClient;

    public DebugPaymentController(MonolithFeignClient monolithFeignClient) {
        this.monolithFeignClient = monolithFeignClient;
    }

    @PostMapping("/simulate-success/saas")
    public ResponseEntity<String> simulateSuccessViaFeign(
            @RequestParam("tenantId") String tenantId,
            @RequestParam("planName") String planName,
            @RequestParam(value = "duration", defaultValue = "MONTHLY") String duration) {

        try {
            monolithFeignClient.activateTenantSubscription(tenantId, planName, duration);
            return ResponseEntity.ok("Successfully sent activation request via Feign Client to Monolith!");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Feign call failed: " + e.getMessage());
        }
    }


    @PostMapping("/simulate-success/members")
    public ResponseEntity<String> createMemberCheckoutSession(
            @RequestParam("userId") String userId,
            @RequestBody SubscriptionRequest request) {

        monolithFeignClient.activateUserMembership(userId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body("Successfully sent member activation request via Feign Client to Monolith!");
    }
}
