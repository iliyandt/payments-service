package demos.springdata.paymentservice.model.entity;

import demos.springdata.paymentservice.model.enums.SubscriptionStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "payment_tenants")
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class PaymentTenant {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true)
    private String tenantId;

    private String name;
    private String businessEmail;

    private String stripeCustomerId;

    private String stripeSubscriptionId;

    private String currentPlanName;
    private String billingPeriod;

    @Enumerated(EnumType.STRING)
    private SubscriptionStatus status;

    private LocalDateTime currentPeriodEnd;
}
