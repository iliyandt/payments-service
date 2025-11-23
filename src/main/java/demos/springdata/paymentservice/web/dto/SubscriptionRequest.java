package demos.springdata.paymentservice.web.dto;

public record SubscriptionRequest(
        Integer allowedVisits,
        String subscriptionPlan,
        String employment
) {

    public SubscriptionRequest(String subscriptionPlan, String employment) {
        this(0, subscriptionPlan, employment);
    }
}
