package demos.springdata.paymentservice.client;

import demos.springdata.paymentservice.web.dto.SubscriptionRequest;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "damilsoft-monolith", url = "${monolith.url}")
public interface MonolithFeignClient {

    @PostMapping("/internal/payments/tenants/{tenantId}/activate")
    void activateTenantSubscription(@PathVariable("tenantId") String tenantId,
                                    @RequestParam("plan") String planName,
                                    @RequestParam("duration") String abonnementDuration);


    @PostMapping("/internal/payments/users/{userId}/memberships/activate")
    void activateUserMembership(@PathVariable("userId") String userId, @RequestBody SubscriptionRequest request);
}