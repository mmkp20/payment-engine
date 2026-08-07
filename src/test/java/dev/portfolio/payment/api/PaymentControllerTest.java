package dev.portfolio.payment.api;

import dev.portfolio.payment.application.PaymentService;
import dev.portfolio.payment.infrastructure.persistence.InMemoryIdempotencyRepository;
import dev.portfolio.payment.infrastructure.persistence.InMemoryPaymentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.http.MediaType;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

public class PaymentControllerTest {

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        InMemoryPaymentRepository paymentRepository = new InMemoryPaymentRepository();
        InMemoryIdempotencyRepository idempotencyRepository = new InMemoryIdempotencyRepository();
        PaymentService paymentService = new PaymentService(paymentRepository, idempotencyRepository);
        PaymentController paymentController = new PaymentController(paymentService);

        mockMvc = MockMvcBuilders.standaloneSetup(paymentController)
                .setControllerAdvice(new GlobalExceptionHandler()).build();
    }

    @Test
    void validRequestCreatesPayment() throws Exception {
        mockMvc.perform(post("/payments")
                .header("Idempotency-Key", "request-1")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                        "amount": 10.99,
                        "currency": "USD"
                        }
                        """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.amount").value(10.99))
                .andExpect(jsonPath("$.currency").value("USD"))
                .andExpect(jsonPath("$.status").value("CREATED"));
    }

    @Test
    void lowercaseCurrencyReturnBadRequest() throws Exception {
        mockMvc.perform(post("/payments")
                        .header("Idempotency-Key", "request-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                        {
                        "amount": 10.99,
                        "currency": "usd"
                        }
                        """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST"))
                .andExpect(jsonPath("$.message")
                        .value("currency must be a 3-letter uppercase code"));
    }

    @Test
    void unknownPaymentReturnsNotFound() throws Exception{
        mockMvc.perform(get("/payments/00000000-0000-0000-0000-000000000000"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("PAYMENT_NOT_FOUND"))
                .andExpect(jsonPath("$.message").value(
                        "Payment not found: " +
                                "00000000-0000-0000-0000-000000000000"
                ));
    }

    @Test
    void repeatedRequestReturnsSamePayment() throws Exception{
        String requestBody = """
                        {
                        "amount": 10.99,
                        "currency": "USD"
                        }
                        """;

        String firstResponse = mockMvc.perform(post("/payments")
                .header("Idempotency-Key", "request-1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        mockMvc.perform(post("/payments")
        .header("Idempotency-Key", "request-1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
                .andExpect(status().isCreated())
                .andExpect(content().json(firstResponse));
    }

    @Test
    void reusedKeyWithDifferentPaymentReturnsConflict() throws Exception{
        mockMvc.perform(post("/payments")
                .header("Idempotency-Key", "conflicting-request")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                        "amount": 10.99,
                        "currency": "USD"
                        }
                        """))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/payments")
                        .header("Idempotency-Key", "conflicting-request")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                "amount": 20.00,
                                "currency": "USD"
                                }
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("IDEMPOTENCY_CONFLICT"))
                .andExpect(jsonPath("$.message").value("Idempotency key was already used with "+
                        "different payment details: conflicting-request"));
    }
}
