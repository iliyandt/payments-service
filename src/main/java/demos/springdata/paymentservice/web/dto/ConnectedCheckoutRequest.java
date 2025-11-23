package demos.springdata.paymentservice.web.dto;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ConnectedCheckoutRequest {
    private UUID userId;
    private String email;
    private String name;
    private String subscriptionPlan;
    private Long amount;
    private Integer allowedVisits;
    private String currency;
    private String employment;
}
