package com.minipay.notification_service;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest
@TestPropertySource(properties = {
		"spring.datasource.url=jdbc:h2:mem:testdb",
		"spring.datasource.driver-class-name=org.h2.Driver",
		"spring.jpa.hibernate.ddl-auto=create-drop",
		"spring.kafka.bootstrap-servers=localhost:9092",
		"spring.kafka.consumer.group-id=test-group",
		"spring.kafka.consumer.auto-offset-reset=earliest",
		"africastalking.username=sandbox",
		"africastalking.api-key=test-key",
		"africastalking.sms-url=https://api.sandbox.africastalking.com/version1/messaging",
		"africastalking.sender-id=",
		"kafka.topics.payment-events=payment.events"
})
class NotificationServiceApplicationTests {

	@Test
	void contextLoads() {
	}
}