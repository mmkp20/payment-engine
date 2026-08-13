package dev.portfolio.payment.infrastructure.messaging;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class SqsPaymentEventSenderTest {

    @Test
    void payloadIsSentToConfiguredQueue() {
        SqsClient sqsClient = mock(SqsClient.class);

        SqsPaymentEventSender sender =
                new SqsPaymentEventSender(
                        sqsClient,
                        "http://localhost:9324/" +
                                "000000000000/payment-processing"
                );

        String payload = """
                {
                  "eventType": "PAYMENT_CREATED"
                }
                """;

        sender.send(payload);

        ArgumentCaptor<SendMessageRequest> requestCaptor =
                ArgumentCaptor.forClass(
                        SendMessageRequest.class
                );

        verify(sqsClient)
                .sendMessage(requestCaptor.capture());

        SendMessageRequest request =
                requestCaptor.getValue();

        assertThat(request.queueUrl()).isEqualTo(
                "http://localhost:9324/" +
                        "000000000000/payment-processing"
        );

        assertThat(request.messageBody())
                .isEqualTo(payload);
    }
}