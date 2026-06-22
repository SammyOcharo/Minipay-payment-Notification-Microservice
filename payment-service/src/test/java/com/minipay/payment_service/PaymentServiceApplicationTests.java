package com.minipay.payment_service;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest
@ActiveProfiles("test")
@TestPropertySource(properties = {
		"spring.datasource.url=jdbc:h2:mem:testdb",
		"spring.datasource.driver-class-name=org.h2.Driver",
		"spring.jpa.hibernate.ddl-auto=create-drop",
		"spring.kafka.bootstrap-servers=localhost:9092",
		"spring.kafka.producer.value-serializer=org.apache.kafka.common.serialization.StringSerializer",
		"jwt.secret=test-secret-key-for-testing-purposes-min-256-bits",
		"jwt.expiration=86400000",
		"mpesa.base-url=https://sandbox.safaricom.co.ke",
		"mpesa.consumer-key=test-key",
		"mpesa.consumer-secret=test-secret",
		"mpesa.shortcode=174379",
		"mpesa.passkey=test-passkey",
		"mpesa.callback-url=http://localhost:8081/api/payments/mpesa/callback",
		"kafka.topics.payment-events=payment.events"
})
class PaymentServiceApplicationTests {

	@Test
	void contextLoads() {
	}
}
