package com.siftnews.common.config;

import com.siftnews.support.AbstractIntegrationTest;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Duration;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
class PrometheusEndpointIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private MeterRegistry meterRegistry;

    @Test
    void exposesBatchMetricsInPrometheusFormat() throws Exception {
        Timer.builder("sift.delivery.dispatch.duration")
                .tag("status", "COMPLETED")
                .register(meterRegistry)
                .record(Duration.ofMillis(100));
        meterRegistry.counter("sift.delivery.tasks.processed", "status", "COMPLETED").increment();

        mockMvc.perform(get("/actuator/prometheus"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_PLAIN))
                .andExpect(content().string(containsString("sift_delivery_dispatch_duration_seconds_bucket")))
                .andExpect(content().string(containsString("sift_delivery_tasks_processed_total")));
    }
}
