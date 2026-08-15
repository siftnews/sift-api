package com.siftnews.source.adapter.in.web;

import com.siftnews.source.application.port.in.LoadtestArticleSeedSummary;
import com.siftnews.source.application.port.in.SeedLoadtestArticlesUseCase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup;

class LoadtestSelectionSeedControllerTest {

    private RecordingSeeder seeder;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        seeder = new RecordingSeeder();
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();
        mockMvc = standaloneSetup(new LoadtestSelectionSeedController(seeder))
                .setValidator(validator)
                .build();
    }

    @Test
    void seedsCandidatesAndReturnsTheSelectionWindow() throws Exception {
        mockMvc.perform(post("/internal/loadtest/v1/selection/seed")
                        .contentType(APPLICATION_JSON)
                        .content("{\"runId\":\"selection-1\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.runId").value("selection-1"))
                .andExpect(jsonPath("$.requestedCount").value(10000))
                .andExpect(jsonPath("$.insertedCount").value(10000))
                .andExpect(jsonPath("$.sourceCount").value(10));
        assertThat(seeder.runId).isEqualTo("selection-1");
    }

    @Test
    void rejectsUnsafeRunIdAtTheHttpBoundary() throws Exception {
        mockMvc.perform(post("/internal/loadtest/v1/selection/seed")
                        .contentType(APPLICATION_JSON)
                        .content("{\"runId\":\"../unsafe\"}"))
                .andExpect(status().isBadRequest());
        assertThat(seeder.runId).isNull();
    }

    private static final class RecordingSeeder implements SeedLoadtestArticlesUseCase {
        private String runId;

        @Override
        public LoadtestArticleSeedSummary seed(String runId) {
            this.runId = runId;
            Instant from = Instant.parse("2026-08-16T00:00:00Z");
            return new LoadtestArticleSeedSummary(runId, 10000, 10000, 10, from, from.plusSeconds(86400));
        }
    }
}
