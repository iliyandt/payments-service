package demos.springdata.paymentservice.model.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Entity
@Table(name = "stripe_connect_accounts")
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class StripeConnectAccount {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    @Column(nullable = false, unique = true)
    private String tenantId;
    @Column(nullable = false)
    private String stripeAccountId;
    private boolean chargesEnabled;
    private boolean payoutsEnabled;
}
