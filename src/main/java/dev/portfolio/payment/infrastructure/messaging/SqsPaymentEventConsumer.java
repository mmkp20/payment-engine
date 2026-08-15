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

@Component
@ConditionalOnProperty(
        name = "app.sqs.consumer-enabled",
        havingValue = "true"
)
public class SqsPaymentEventConsumer {

    private static final Logger log =
            LoggerFactory.getLogger(SqsPaymentEventConsumer.class);

    private final SqsClient sqsClient;
    private final ObjectMapper objectMapper;
    private final PaymentProcessor paymentProcessor;
    private final String queueUrl;

    public SqsPaymentEventConsumer(
            SqsClient sqsClient,
            ObjectMapper objectMapper,
            PaymentProcessor paymentProcessor,
            @Value("${app.sqs.queue-url}") String queueUrl
    ) {
        this.sqsClient = sqsClient;
        this.objectMapper = objectMapper;
        this.paymentProcessor = paymentProcessor;
        this.queueUrl = queueUrl;
    }

    @Scheduled(
            fixedDelayString =
                    "${app.sqs.consumer-poll-delay-ms:1000}"
    )
    public void pollMessages() {
        ReceiveMessageRequest request =
                ReceiveMessageRequest.builder()
                        .queueUrl(queueUrl)
                        .maxNumberOfMessages(10)
                        .waitTimeSeconds(1)
                        .build();

        for (Message message :
                sqsClient.receiveMessage(request).messages()) {
            processMessage(message);
        }
    }

    private void processMessage(Message message) {
        try {
            PaymentCreatedEvent event =
                    objectMapper.readValue(
                            message.body(),
                            PaymentCreatedEvent.class
                    );

            paymentProcessor.processPayment(event.paymentId());

            sqsClient.deleteMessage(
                    DeleteMessageRequest.builder()
                            .queueUrl(queueUrl)
                            .receiptHandle(message.receiptHandle())
                            .build()
            );

            log.info(
                    "Processed payment event: paymentId={}",
                    event.paymentId()
            );
        } catch (Exception exception) {
            log.error(
                    "Failed to process SQS message: messageId={}",
                    message.messageId(),
                    exception
            );
        }
    }
}