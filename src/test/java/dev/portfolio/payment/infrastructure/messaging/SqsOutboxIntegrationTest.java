package dev.portfolio.payment.infrastructure.messaging;

import dev.portfolio.payment.application.PaymentService;
import dev.portfolio.payment.domain.*;
import dev.portfolio.payment.infrastructure.persistence.OutboxEventEntity;
import dev.portfolio.payment.infrastructure.persistence.SpringDataOutboxRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.CreateQueueRequest;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageRequest;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageResponse;
import dev.portfolio.payment.application.PaymentProcessor;
import dev.portfolio.payment.infrastructure.observability.PaymentMetrics;
import tools.jackson.databind.ObjectMapper;
import software.amazon.awssdk.services.sqs.model.DeleteMessageRequest;

import java.math.BigDecimal;
import java.util.Currency;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers
@SpringBootTest
class SqsOutboxIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:17-alpine");

    @Container
    static GenericContainer<?> elasticmq =
            new GenericContainer<>(DockerImageName.parse("softwaremill/elasticmq-native:1.7.1"))
                    .withExposedPorts(9324);

    @DynamicPropertySource
    static void configureSqs(DynamicPropertyRegistry registry) {
        registry.add("app.sqs.endpoint", () -> elasticmqEndpoint());

        registry.add("app.sqs.queue-url", () -> elasticmqEndpoint()
                        + "/000000000000/payment-processing");

        registry.add("app.outbox.publisher.enabled", () -> "false");
    }

    private static String elasticmqEndpoint() {
        return "http://localhost:" + elasticmq.getMappedPort(9324);
    }

    @Autowired
    private PaymentService paymentService;

    @Autowired
    private OutboxPublisher outboxPublisher;

    @Autowired
    private SpringDataOutboxRepository outboxRepository;

    @Autowired
    private SqsClient sqsClient;

    @Value("${app.sqs.queue-url}")
    private String queueUrl;

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private PaymentProcessor paymentProcessor;

    @Autowired
    private PaymentMetrics paymentMetrics;

    @Autowired
    private ObjectMapper objectMapper;

    @BeforeEach
    void createQueue() {
        sqsClient.createQueue( CreateQueueRequest.builder()
                        .queueName("payment-processing")
                        .build()
        );
    }

    @Test
    void pendingOutboxEventIsPublishedToSqs() {
        Payment payment = paymentService.createPayment(
                new IdempotencyKey("sqs-integration-request"),
                new Money(new BigDecimal("55.00"), Currency.getInstance("USD")));

        int publishedCount = outboxPublisher.publishPendingEvents();

        assertThat(publishedCount).isEqualTo(1);

        ReceiveMessageResponse response = sqsClient.receiveMessage(ReceiveMessageRequest.builder()
                                .queueUrl(queueUrl)
                                .maxNumberOfMessages(1)
                                .build());

        assertThat(response.messages()).hasSize(1);
        assertThat(response.messages().get(0).body())
                .contains(payment.getId().toString())
                .contains("\"amount\": 55.00")
                .contains("\"currency\": \"USD\"");

        OutboxEventEntity event = outboxRepository.findAllByAggregateId(payment.getId()).get(0);

        assertThat(event.getStatus()).isEqualTo(OutboxStatus.PUBLISHED);
        assertThat(event.getPublishedAt()).isNotNull();

        sqsClient.deleteMessage(DeleteMessageRequest.builder()
                        .queueUrl(queueUrl)
                        .receiptHandle(response.messages().get(0).receiptHandle())
                        .build());
    }

    @Test
    void paymentFlowsThroughOutboxAndSqsToSucceeded() {
        Payment payment = paymentService.createPayment(
                new IdempotencyKey("end-to-end-request"),
                new Money(new BigDecimal("77.00"),
                        Currency.getInstance("USD")));

        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.CREATED);

        int publishedCount =outboxPublisher.publishPendingEvents();

        assertThat(publishedCount).isEqualTo(1);

        SqsPaymentEventConsumer consumer = new SqsPaymentEventConsumer(
                        sqsClient,
                        objectMapper,
                        paymentProcessor,
                        paymentMetrics,
                        queueUrl,
                        3);

        consumer.pollMessages();

        Payment processedPayment = paymentRepository.findById(payment.getId()).orElseThrow();

        assertThat(processedPayment.getStatus()).isEqualTo(PaymentStatus.SUCCEEDED);

        OutboxEventEntity outboxEvent = outboxRepository
                .findAllByAggregateId(payment.getId())
                .get(0);

        assertThat(outboxEvent.getStatus()).isEqualTo(OutboxStatus.PUBLISHED);

        ReceiveMessageResponse remainingMessages = sqsClient.receiveMessage(ReceiveMessageRequest.builder()
                                .queueUrl(queueUrl)
                                .maxNumberOfMessages(10)
                                .build());

        assertThat(remainingMessages.messages()).isEmpty();
    }

    @Test
    void duplicateRequestProducesSinglePaymentAndEvent() {
        IdempotencyKey key = new IdempotencyKey("end-to-end-idempotency-request");

        Money money = new Money( new BigDecimal("99.00"),
                Currency.getInstance("USD"));

        Payment firstPayment = paymentService.createPayment(key, money);
        Payment duplicatePayment = paymentService.createPayment(key, money);

        assertThat(duplicatePayment.getId()).isEqualTo(firstPayment.getId());
        assertThat(outboxRepository.findAllByAggregateId(
                        firstPayment.getId())).hasSize(1);

        int publishedCount =outboxPublisher.publishPendingEvents();

        assertThat(publishedCount).isEqualTo(1);

        SqsPaymentEventConsumer consumer = new SqsPaymentEventConsumer(
                        sqsClient,
                        objectMapper,
                        paymentProcessor,
                        paymentMetrics,
                        queueUrl,
                        3);

        consumer.pollMessages();

        Payment processedPayment = paymentRepository
                .findById(firstPayment.getId())
                .orElseThrow();

        assertThat(processedPayment.getStatus())
                .isEqualTo(PaymentStatus.SUCCEEDED);

        var outboxEvents = outboxRepository.findAllByAggregateId(
                        firstPayment.getId());

        assertThat(outboxEvents).hasSize(1);
        assertThat(outboxEvents.get(0).getStatus()).isEqualTo(OutboxStatus.PUBLISHED);
    }
}