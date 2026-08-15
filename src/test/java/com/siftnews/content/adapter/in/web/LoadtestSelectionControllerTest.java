package com.siftnews.content.adapter.in.web;

import com.siftnews.content.application.port.in.SelectionJobRunSummary;
import com.siftnews.content.application.port.in.SelectionJobRunner;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import java.time.Instant;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup;

class LoadtestSelectionControllerTest {

    private static final Instant FROM = Instant.parse("2026-08-15T00:00:00Z");
    private static final Instant TO = Instant.parse("2026-08-16T00:00:00Z");

    private RecordingRunner runner;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        runner = new RecordingRunner();
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();
        mockMvc = standaloneSetup(new LoadtestSelectionController(runner))
                .setMessageConverters(new MappingJackson2HttpMessageConverter(JsonMapper.builder()
                        .addModule(new JavaTimeModule())
                        .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
                        .build()))
                .setValidator(validator)
                .build();
    }

    @Test
    void runsSelectionJobWithExplicitWindowAndReturnsSummary() throws Exception {
        mockMvc.perform(post("/internal/loadtest/v1/selection")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"topicId":7,"runDate":"2026-08-16","from":"2026-08-15T00:00:00Z","to":"2026-08-16T00:00:00Z"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.executionId").value(78))
                .andExpect(jsonPath("$.topicId").value(7))
                .andExpect(jsonPath("$.runDate").value("2026-08-16"))
                .andExpect(jsonPath("$.windowFrom").value("2026-08-15T00:00:00Z"))
                .andExpect(jsonPath("$.windowTo").value("2026-08-16T00:00:00Z"))
                .andExpect(jsonPath("$.status").value("COMPLETED"))
                .andExpect(jsonPath("$.elapsedMs").value(4567));
        assertThat(runner.topicId).isEqualTo(7L);
        assertThat(runner.from).isEqualTo(FROM);
        assertThat(runner.to).isEqualTo(TO);
    }

    @Test
    void rejectsAnInvertedWindowAtTheHttpBoundary() throws Exception {
        mockMvc.perform(post("/internal/loadtest/v1/selection")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"topicId":7,"runDate":"2026-08-16","from":"2026-08-16T00:00:00Z","to":"2026-08-15T00:00:00Z"}
                                """))
                .andExpect(status().isBadRequest());
        assertThat(runner.topicId).isNull();
    }

    private static final class RecordingRunner implements SelectionJobRunner {
        private Long topicId;
        private Instant from;
        private Instant to;

        @Override
        public SelectionJobRunSummary run(Long topicId, LocalDate runDate, Instant from, Instant to) {
            this.topicId = topicId;
            this.from = from;
            this.to = to;
            return new SelectionJobRunSummary(78L, topicId, runDate, from, to,
                    "COMPLETED", "COMPLETED", 4567);
        }
    }
}
