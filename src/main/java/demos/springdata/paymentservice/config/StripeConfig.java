package demos.springdata.paymentservice.config;

import com.stripe.Stripe;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
public class StripeConfig {
    private final StripeProperties props;

    public StripeConfig(StripeProperties props) {
        this.props = props;
    }

    @PostConstruct
    public void init() {
        Stripe.apiKey = props.getApiKey();
    }
}
