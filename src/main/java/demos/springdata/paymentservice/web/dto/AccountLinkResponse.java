package demos.springdata.paymentservice.web.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class AccountLinkResponse {
    private String url;
    private Long created;
    private Long expiresAt;
}
