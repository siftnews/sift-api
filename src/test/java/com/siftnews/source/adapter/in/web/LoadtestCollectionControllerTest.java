package com.siftnews.source.adapter.in.web;

import com.siftnews.source.application.port.in.CollectionJobRunSummary;
import com.siftnews.source.application.port.in.CollectionJobRunner;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup;

class LoadtestCollectionControllerTest {

    private RecordingRunner runner;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        runner = new RecordingRunner();
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();
        mockMvc = standaloneSetup(new LoadtestCollectionController(runner))
                .setValidator(validator)
                .build();
    }

    @Test
    void runsCollectionJobAndReturnsBatchSummary() throws Exception {
        mockMvc.perform(post("/internal/loadtest/v1/collection")
                        .contentType(APPLICATION_JSON)
                        .content("{\"runId\":\"collection-1\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.runId").value("collection-1"))
                .andExpect(jsonPath("$.executionId").value(77))
                .andExpect(jsonPath("$.status").value("COMPLETED"))
                .andExpect(jsonPath("$.sourceReadCount").value(10))
                .andExpect(jsonPath("$.sourceWriteCount").value(10))
                .andExpect(jsonPath("$.sourceSkipCount").value(0))
                .andExpect(jsonPath("$.elapsedMs").value(1234));
        assertThat(runner.runs).isEqualTo(1);
        assertThat(runner.receivedRunId).isEqualTo("collection-1");
    }

    private static final class RecordingRunner implements CollectionJobRunner {
        private int runs;
        private String receivedRunId;

        @Override
        public CollectionJobRunSummary run() {
            return run("generated-run");
        }

        @Override
        public CollectionJobRunSummary run(String runId) {
            runs++;
            receivedRunId = runId;
            return new CollectionJobRunSummary(runId, 77L, "COMPLETED", "COMPLETED", 10, 10, 0, 1234);
        }
    }
}
