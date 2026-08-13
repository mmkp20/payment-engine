package dev.portfolio.payment.infrastructure.messaging;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.sqs.SqsClient;

import java.net.URI;

@Configuration
public class SqsConfiguration {

    @Bean
    public SqsClient sqsClient(@Value("${app.aws.region}") String region, @Value("${app.sqs.endpoint}") String endpoint) {
        return SqsClient.builder().region(Region.of(region))
                .endpointOverride(URI.create(endpoint))
                .credentialsProvider(StaticCredentialsProvider.create( AwsBasicCredentials
                                .create("local","local"))).build();
    }
}