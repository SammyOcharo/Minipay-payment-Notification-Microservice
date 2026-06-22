package com.minipay.api_gateway;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;


@SpringBootTest
@TestPropertySource(properties = {
		"spring.datasource.url=jdbc:h2:mem:testdb",
		"spring.datasource.driver-class-name=org.h2.Driver",
		"spring.jpa.hibernate.ddl-auto=create-drop",
		"spring.cloud.gateway.routes[0].id=payment-service",
		"spring.cloud.gateway.routes[0].uri=http://localhost:8081",
		"spring.cloud.gateway.routes[0].predicates[0]=Path=/api/payments/**",
		"spring.cloud.gateway.routes[1].id=notification-service",
		"spring.cloud.gateway.routes[1].uri=http://localhost:8082",
		"spring.cloud.gateway.routes[1].predicates[0]=Path=/api/notifications/**",
		"spring.security.oauth2.client.registration.google.client-id=test-client-id",
		"spring.security.oauth2.client.registration.google.client-secret=test-secret",
		"jwt.secret=minipay-super-secret-key-for-testing-purposes-min-256-bits",
		"jwt.expiration=86400000",
		"jwt.refresh-expiration=604800000"
})
class ApiGatewayApplicationTests {

	@Test
	void contextLoads() {
	}
}