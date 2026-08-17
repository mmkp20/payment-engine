package dev.portfolio.payment.api;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;

class CorrelationIdFilterTest {

    private final CorrelationIdFilter filter =
            new CorrelationIdFilter();

    @Test
    void providedCorrelationIdIsReturned() throws Exception {
        MockHttpServletRequest request =
                new MockHttpServletRequest();

        request.addHeader(
                CorrelationIdFilter.HEADER_NAME,
                "request-123"
        );

        MockHttpServletResponse response =
                new MockHttpServletResponse();

        filter.doFilter(
                request,
                response,
                new MockFilterChain()
        );

        assertThat(
                response.getHeader(
                        CorrelationIdFilter.HEADER_NAME
                )
        ).isEqualTo("request-123");
    }

    @Test
    void missingCorrelationIdIsGenerated() throws Exception {
        MockHttpServletRequest request =
                new MockHttpServletRequest();

        MockHttpServletResponse response =
                new MockHttpServletResponse();

        filter.doFilter(
                request,
                response,
                new MockFilterChain()
        );

        assertThat(
                response.getHeader(
                        CorrelationIdFilter.HEADER_NAME
                )
        ).isNotBlank();
    }
}