package demos.springdata.paymentservice.web.dto;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ConnectedCheckoutRequest {
    private Long userId;
    private String subscriptionPlan;
    private Long amount;
    private Integer allowedVisits;
    private String currency;
    private String employment;
}
