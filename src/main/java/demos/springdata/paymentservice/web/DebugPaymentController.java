package demos.springdata.paymentservice.web;

import demos.springdata.paymentservice.client.MonolithFeignClient;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

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
}
