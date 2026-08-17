package dev.portfolio.payment.infrastructure.messaging;

import dev.portfolio.payment.application.PaymentCreatedEvent;
import dev.portfolio.payment.application.PaymentProcessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.DeleteMessageRequest;
import software.amazon.awssdk.services.sqs.model.Message;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageRequest;
import tools.jackson.databind.ObjectMapper;
import software.amazon.awssdk.services.sqs.model.MessageSystemAttributeName;
import dev.portfolio.payment.infrastructure.observability.PaymentMetrics;

@Component
@ConditionalOnProperty(
        name = "app.sqs.consumer-enabled",
        havingValue = "true"
)
public class SqsPaymentEventConsumer {

    private static final Logger log = LoggerFactory.getLogger(SqsPaymentEventConsumer.class);

    private final SqsClient sqsClient;
    private final ObjectMapper objectMapper;
    private final PaymentProcessor paymentProcessor;
    private final String queueUrl;
    private final int maxReceiveCount;
    private final PaymentMetrics paymentMetrics;

    public SqsPaymentEventConsumer(
            SqsClient sqsClient,
            ObjectMapper objectMapper,
            PaymentProcessor paymentProcessor,
            PaymentMetrics paymentMetrics,
            @Value("${app.sqs.queue-url}") String queueUrl,
            @Value("${app.sqs.max-receive-count:3}")int maxReceiveCount) {

        this.sqsClient = sqsClient;
        this.objectMapper = objectMapper;
        this.paymentProcessor = paymentProcessor;
        this.queueUrl = queueUrl;
        this.maxReceiveCount = maxReceiveCount;
        this.paymentMetrics = paymentMetrics;
    }

    @Scheduled(fixedDelayString ="${app.sqs.consumer-poll-delay-ms:1000}")
    public void pollMessages() {

        ReceiveMessageRequest request = ReceiveMessageRequest.builder()
                                        .queueUrl(queueUrl)
                                        .maxNumberOfMessages(10)
                                        .waitTimeSeconds(1)
                                        .messageSystemAttributeNames(
                                                MessageSystemAttributeName
                                                        .APPROXIMATE_RECEIVE_COUNT
                                        )
                                        .build();

        for (Message message : sqsClient.receiveMessage(request).messages()) {
            processMessage(message);
        }
    }

    private void processMessage(Message message) {
        PaymentCreatedEvent event = null;
        boolean processingCompleted = false;
        int receiveCount = getReceiveCount(message);

        try {
            event = objectMapper.readValue(
                    message.body(),
                    PaymentCreatedEvent.class
            );

            paymentProcessor.processPayment(event.paymentId());
            processingCompleted = true;

            sqsClient.deleteMessage(
                    DeleteMessageRequest.builder()
                            .queueUrl(queueUrl)
                            .receiptHandle(message.receiptHandle())
                            .build()
            );

            paymentMetrics.recordSuccess();

            log.info(
                    "Processed payment event: paymentId={}",
                    event.paymentId()
            );
        } catch (Exception exception) {
            if (receiveCount >= maxReceiveCount) {
                if (event != null && !processingCompleted) {
                    markPaymentFailed(event);
                }

                paymentMetrics.recordFinalFailure();
            } else {
                paymentMetrics.recordRetry();
            }

            log.error(
                    "Failed to process SQS message: " +
                            "messageId={}, attempt={}/{}",
                    message.messageId(),
                    receiveCount,
                    maxReceiveCount,
                    exception
            );
        }
    }

    private int getReceiveCount(Message message) {

        String receiveCount = message.attributes().get(MessageSystemAttributeName.APPROXIMATE_RECEIVE_COUNT);

        if (receiveCount == null) {
            return 1;
        }

        try {
            return Integer.parseInt(receiveCount);
        } catch (NumberFormatException exception) {
            return 1;
        }
    }

    private void markPaymentFailed(PaymentCreatedEvent event) {
        try {
            paymentProcessor.failPayment(event.paymentId());
            log.warn("Payment marked as failed after retries: " + "paymentId={}", event.paymentId());

        } catch (Exception exception) {
            log.error("Could not mark payment as failed: " + "paymentId={}", event.paymentId(), exception);

        }
    }
}