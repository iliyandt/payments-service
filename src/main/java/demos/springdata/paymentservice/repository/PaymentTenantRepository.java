package demos.springdata.paymentservice.repository;

import demos.springdata.paymentservice.model.entity.PaymentTenant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface PaymentTenantRepository extends JpaRepository<PaymentTenant, UUID> {
    Optional<PaymentTenant> findByTenantId(String tenantId);
}
