package demos.springdata.paymentservice.web.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class CheckoutRequest {
    private String tenantId;
    private String tenantName;
    private String businessEmail;
    private String plan;
    private Long amount;
    private String currency;
    private String abonnementDuration;
}
