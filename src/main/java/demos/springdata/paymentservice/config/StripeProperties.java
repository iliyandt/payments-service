package demos.springdata.paymentservice.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;


@ConfigurationProperties(prefix = "stripe")
@Data
public class StripeProperties {
    private String apiKey;
    private String webhookSecret;
}
