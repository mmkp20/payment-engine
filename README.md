# Payment Engine

A project for building a resilient, event-driven payment processing system with Java, Spring Boot and AWS

> This project processes simulated transactions only. It does not handle real money or customer data.


## Current Status
- Spring Boot Application initialized
- Health endpoint available at '/actuator/health'
- Payment lifecycle defined with 'CREATED', 'PROCESSING', 'SUCCEEDED', and 'FAILED'
- Invalid payment state transitions are rejected
- Payment domain behavior is covered by unit tests

## Tech Stack
- Java 17
- Spring Boot 4.0.7
- Spring Boot Actuator
- Maven
- JUnit