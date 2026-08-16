package com.siftnews.common.config;

import com.siftnews.support.AbstractIntegrationTest;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
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

    @Value("${management.observations.long-task-timer.enabled:true}")
    private boolean observationLongTaskTimerEnabled;

    @Test
    void disablesObservationLongTaskTimerForSpringBatchMetrics() {
        assertThat(observationLongTaskTimerEnabled).isFalse();
    }

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

    @Test
    void exposesSpringBatchJobAndStepTagsInPrometheusFormat() throws Exception {
        Timer.builder("spring.batch.job")
                .tag("spring.batch.job.name", "selectionJob")
                .tag("spring.batch.job.status", "COMPLETED")
                .register(meterRegistry)
                .record(Duration.ofMillis(100));
        Timer.builder("spring.batch.step")
                .tag("spring.batch.step.name", "normalizeDedupStep")
                .tag("spring.batch.step.job.name", "selectionJob")
                .tag("spring.batch.step.status", "COMPLETED")
                .register(meterRegistry)
                .record(Duration.ofMillis(100));

        mockMvc.perform(get("/actuator/prometheus"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("spring_batch_job_seconds_count")))
                .andExpect(content().string(containsString("spring_batch_job_name=\"selectionJob\"")))
                .andExpect(content().string(containsString("spring_batch_step_seconds_count")))
                .andExpect(content().string(containsString("spring_batch_step_job_name=\"selectionJob\"")));
    }
}
