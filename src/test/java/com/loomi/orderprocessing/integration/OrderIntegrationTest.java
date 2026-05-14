package com.loomi.orderprocessing.integration;

import com.loomi.orderprocessing.dto.CreateOrderRequest;
import com.loomi.orderprocessing.dto.OrderItemRequest;
import com.loomi.orderprocessing.dto.OrderResponse;
import com.loomi.orderprocessing.model.enums.OrderStatus;
import com.loomi.orderprocessing.repository.OrderRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Testcontainers
class OrderIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15")
            .withDatabaseName("testdb")
            .withUsername("test")
            .withPassword("test");

    @Container
    static KafkaContainer kafka = new KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:7.5.0"));

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.kafka.bootstrap-servers", kafka::getBootstrapServers);
    }

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private OrderRepository orderRepository;

    @Test
    void shouldCreateOrderAndProcessAsync() {
        // Arrange
        var request = new CreateOrderRequest("customer-123", List.of(
                new OrderItemRequest("LAPTOP-PRO-2024", 1)
        ));

        // Act
        var response = restTemplate.postForEntity("/api/orders", request, OrderResponse.class);

        // Assert - order created with PENDING status
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().status()).isEqualTo(OrderStatus.PENDING);

        String orderId = response.getBody().orderId();

        // Assert - order processed asynchronously by Kafka consumer
        await().atMost(15, TimeUnit.SECONDS).untilAsserted(() -> {
            var order = orderRepository.findById(orderId).orElseThrow();
            assertThat(order.getStatus()).isEqualTo(OrderStatus.PROCESSED);
        });
    }

    @Test
    void shouldReturn404ForNonExistentOrder() {
        // Act
        var response = restTemplate.getForEntity("/api/orders/non-existent-id", OrderResponse.class);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void shouldReturn400WhenRequestIsInvalid() {
        // Arrange - empty items list
        var request = new CreateOrderRequest("customer-123", List.of());

        // Act
        var response = restTemplate.postForEntity("/api/orders", request, Object.class);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void shouldReturnOrdersByCustomer() {
        // Arrange
        var request = new CreateOrderRequest("customer-list-test", List.of(
                new OrderItemRequest("BOOK-CC-001", 1)
        ));
        restTemplate.postForEntity("/api/orders", request, OrderResponse.class);

        // Act
        var response = restTemplate.getForEntity(
                "/api/orders?customerId=customer-list-test", OrderResponse[].class);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).hasSizeGreaterThanOrEqualTo(1);
    }
}
