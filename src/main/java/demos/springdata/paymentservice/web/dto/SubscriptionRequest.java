package demos.springdata.paymentservice.web.dto;

public record SubscriptionRequest(
        Integer allowedVisits,
        String subscriptionPlan,
        String employment
) {

}
