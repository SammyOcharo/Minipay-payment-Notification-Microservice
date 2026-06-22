package com.minipay.payment_service.mapper;

import com.minipay.payment_service.dto.PaymentEvent;
import com.minipay.payment_service.dto.PaymentResponse;
import com.minipay.payment_service.model.Payment;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface PaymentMapper {

    PaymentResponse toResponse(Payment payment);

    @Mapping(source = "id", target = "paymentId")
    @Mapping(source = "userEmail", target = "userEmail")
    @Mapping(source = "phoneNumber", target = "phoneNumber")
    PaymentEvent toEvent(Payment payment);
}