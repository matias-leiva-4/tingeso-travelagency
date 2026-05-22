package com.travelagency.backend.repository;

import com.travelagency.backend.model.PaymentEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PaymentRepository extends JpaRepository<PaymentEntity,Long> {
    boolean existsByReservationId(Long reservationId);
    Optional<PaymentEntity> findByReservationId(Long reservationId);

}
