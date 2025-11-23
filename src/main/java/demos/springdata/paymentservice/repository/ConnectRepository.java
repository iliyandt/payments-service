package demos.springdata.paymentservice.repository;

import demos.springdata.paymentservice.model.entity.StripeConnectAccount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface ConnectRepository extends JpaRepository<StripeConnectAccount, UUID> {
    Optional<StripeConnectAccount> findByStripeAccountId(String stripeAccountId);

    Optional<StripeConnectAccount> findByTenantId(String tenantId);
}
