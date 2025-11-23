package demos.springdata.paymentservice.model.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Entity
@Table(name = "payment_customers")
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class PaymentCustomer {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String userId;

    @Column(nullable = false)
    private String stripeCustomerId;

    @Column(nullable = false)
    private String stripeConnectedAccountId;
}
