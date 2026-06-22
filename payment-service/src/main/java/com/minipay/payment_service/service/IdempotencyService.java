package com.minipay.payment_service.service;


import com.fasterxml.jackson.databind.ObjectMapper;
import com.minipay.payment_service.model.IdempotencyRecord;
import com.minipay.payment_service.repository.IdempotencyRecordRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class IdempotencyService {

    private final IdempotencyRecordRepository idempotencyRecordRepository;
    private final ObjectMapper objectMapper;

    public Optional<IdempotencyRecord> findExistingRecord(
            String idempotencyKey) {
        return idempotencyRecordRepository
                .findByIdempotencyKey(idempotencyKey)
                .filter(record -> record.getExpiresAt()
                        .isAfter(LocalDateTime.now()));
    }

    @Transactional
    public IdempotencyRecord saveRecord(
            String idempotencyKey,
            String requestHash,
            Object responseBody,
            Integer responseStatus,
            String paymentId) {
        try {
            String responseJson = objectMapper
                    .writeValueAsString(responseBody);

            IdempotencyRecord record = IdempotencyRecord.builder()
                    .idempotencyKey(idempotencyKey)
                    .requestHash(requestHash)
                    .responseBody(responseJson)
                    .responseStatus(responseStatus)
                    .paymentId(paymentId)
                    .expiresAt(LocalDateTime.now().plusHours(24))
                    .build();

            return idempotencyRecordRepository.save(record);

        } catch (Exception e) {
            log.error("Failed to save idempotency record: {}",
                    e.getMessage());
            throw new RuntimeException(
                    "Failed to save idempotency record", e);
        }
    }

    public String hashRequest(Object request) {
        try {
            String json = objectMapper.writeValueAsString(request);
            return String.valueOf(json.hashCode());
        } catch (Exception e) {
            throw new RuntimeException("Failed to hash request", e);
        }
    }
}