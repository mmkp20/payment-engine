package dev.portfolio.payment.infrastructure.messaging;

import dev.portfolio.payment.application.PaymentCreatedEvent;
import dev.portfolio.payment.application.PaymentProcessor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.DeleteMessageRequest;
import software.amazon.awssdk.services.sqs.model.Message;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageRequest;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageResponse;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import software.amazon.awssdk.services.sqs.model.MessageSystemAttributeName;
import java.util.Map;

class SqsPaymentEventConsumerTest {

    private static final String QUEUE_URL =
            "http://localhost:9324/000000000000/payment-processing";

    private SqsClient sqsClient;
    private ObjectMapper objectMapper;
    private PaymentProcessor paymentProcessor;
    private SqsPaymentEventConsumer consumer;

    @BeforeEach
    void setUp() {
        sqsClient = mock(SqsClient.class);
        objectMapper = mock(ObjectMapper.class);
        paymentProcessor = mock(PaymentProcessor.class);

        consumer = new SqsPaymentEventConsumer(
                sqsClient,
                objectMapper,
                paymentProcessor,
                QUEUE_URL,
                3);
    }

    @Test
    void successfulMessageIsProcessedAndDeleted() throws Exception {
        UUID paymentId = UUID.randomUUID();

        PaymentCreatedEvent event = new PaymentCreatedEvent(
                UUID.randomUUID(),
                paymentId,
                new BigDecimal("55.00"),
                "USD",
                Instant.now()
        );

        Message message = Message.builder()
                .messageId("message-1")
                .receiptHandle("receipt-1")
                .body("{}")
                .build();

        when(sqsClient.receiveMessage(
                any(ReceiveMessageRequest.class)
        )).thenReturn(
                ReceiveMessageResponse.builder()
                        .messages(message)
                        .build()
        );

        when(objectMapper.readValue(
                "{}",
                PaymentCreatedEvent.class
        )).thenReturn(event);

        consumer.pollMessages();

        verify(paymentProcessor).processPayment(paymentId);

        ArgumentCaptor<DeleteMessageRequest> requestCaptor =
                ArgumentCaptor.forClass(DeleteMessageRequest.class);

        verify(sqsClient).deleteMessage(requestCaptor.capture());

        assertThat(requestCaptor.getValue().queueUrl())
                .isEqualTo(QUEUE_URL);
        assertThat(requestCaptor.getValue().receiptHandle())
                .isEqualTo("receipt-1");
    }

    @Test
    void failedMessageIsNotDeleted() throws Exception {
        UUID paymentId = UUID.randomUUID();

        PaymentCreatedEvent event = new PaymentCreatedEvent(
                UUID.randomUUID(),
                paymentId,
                new BigDecimal("55.00"),
                "USD",
                Instant.now()
        );

        Message message = Message.builder()
                .messageId("message-2")
                .receiptHandle("receipt-2")
                .body("{}")
                .build();

        when(sqsClient.receiveMessage(
                any(ReceiveMessageRequest.class)
        )).thenReturn(
                ReceiveMessageResponse.builder()
                        .messages(message)
                        .build()
        );

        when(objectMapper.readValue(
                "{}",
                PaymentCreatedEvent.class
        )).thenReturn(event);

        when(paymentProcessor.processPayment(paymentId))
                .thenThrow(
                        new IllegalStateException(
                                "Processing failed"
                        )
                );

        consumer.pollMessages();

        verify(paymentProcessor).processPayment(paymentId);

        verify(sqsClient, never()).deleteMessage(
                any(DeleteMessageRequest.class)
        );

        verify(paymentProcessor, never())
                .failPayment(any(UUID.class));
    }

    @Test
    void finalFailedAttemptMarksPaymentFailedAndKeepsMessage()
            throws Exception {
        UUID paymentId = UUID.randomUUID();

        PaymentCreatedEvent event = new PaymentCreatedEvent(
                UUID.randomUUID(),
                paymentId,
                new BigDecimal("55.00"),
                "USD",
                Instant.now()
        );

        Message message = Message.builder()
                .messageId("message-3")
                .receiptHandle("receipt-3")
                .body("{}")
                .attributes(
                        Map.of(
                                MessageSystemAttributeName
                                        .APPROXIMATE_RECEIVE_COUNT,
                                "3"
                        )
                )
                .build();

        when(sqsClient.receiveMessage(
                any(ReceiveMessageRequest.class)
        )).thenReturn(
                ReceiveMessageResponse.builder()
                        .messages(message)
                        .build()
        );

        when(objectMapper.readValue(
                "{}",
                PaymentCreatedEvent.class
        )).thenReturn(event);

        when(paymentProcessor.processPayment(paymentId))
                .thenThrow(
                        new IllegalStateException(
                                "Processing failed"
                        )
                );

        consumer.pollMessages();

        verify(paymentProcessor).processPayment(paymentId);
        verify(paymentProcessor).failPayment(paymentId);

        verify(sqsClient, never()).deleteMessage(
                any(DeleteMessageRequest.class)
        );
    }
}