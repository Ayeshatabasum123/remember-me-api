package com.rememberme.api.repository;

import com.rememberme.api.entity.PaymentEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PaymentEventRepository extends JpaRepository<PaymentEvent, Long> {
    boolean existsByStripeEventId(String stripeEventId);
    Optional<PaymentEvent> findByStripeEventId(String stripeEventId);
}
