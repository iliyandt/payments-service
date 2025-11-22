package demos.springdata.paymentservice.repository;

import demos.springdata.paymentservice.model.entity.PaymentCustomer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface PaymentCustomerRepository extends JpaRepository<PaymentCustomer, UUID> {
}
