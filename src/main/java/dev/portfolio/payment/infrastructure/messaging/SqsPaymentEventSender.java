package dev.portfolio.payment.infrastructure.messaging;

import dev.portfolio.payment.application.PaymentEventSender;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;

@Component
public class SqsPaymentEventSender
        implements PaymentEventSender {

    private final SqsClient sqsClient;
    private final String queueUrl;

    public SqsPaymentEventSender(SqsClient sqsClient,
            @Value("${app.sqs.queue-url}") String queueUrl) {
        this.sqsClient = sqsClient;
        this.queueUrl = queueUrl;
    }

    @Override
    public void send(String payload) {
        SendMessageRequest request = SendMessageRequest.builder()
                        .queueUrl(queueUrl)
                        .messageBody(payload)
                        .build();

        sqsClient.sendMessage(request);
    }
}