package com.minipay.notification_service.repository;

import com.minipay.notification_service.enums.NotificationStatus;
import com.minipay.notification_service.model.Notification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface NotificationRepository
        extends JpaRepository<Notification, String> {

    Optional<Notification> findByPaymentId(String paymentId);

    List<Notification> findByStatus(NotificationStatus status);

    Page<Notification> findByUserEmail(
            String userEmail, Pageable pageable);

    boolean existsByPaymentId(String paymentId);
}