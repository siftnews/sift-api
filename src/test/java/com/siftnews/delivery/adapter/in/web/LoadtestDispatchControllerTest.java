package com.siftnews.delivery.adapter.in.web;

import com.siftnews.delivery.application.port.in.DispatchJobRunSummary;
import com.siftnews.delivery.application.port.in.DispatchJobRunner;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Profile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup;

class LoadtestDispatchControllerTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(LoadtestControllerConfiguration.class);

    private RecordingDispatchJobRunner runner;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        runner = new RecordingDispatchJobRunner();
        LoadtestDispatchController controller = new LoadtestDispatchController(runner);
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();
        mockMvc = standaloneSetup(controller)
                .setValidator(validator)
                .build();
    }

    @Test
    void runsDispatchJobAndReturnsExecutionSummary() throws Exception {
        mockMvc.perform(post("/internal/loadtest/v1/dispatch")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"issueId":11,"topicId":12,"preferredSendHour":9}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.issueId").value(11))
                .andExpect(jsonPath("$.topicId").value(12))
                .andExpect(jsonPath("$.preferredSendHour").value(9))
                .andExpect(jsonPath("$.executionId").value(42))
                .andExpect(jsonPath("$.status").value("COMPLETED"))
                .andExpect(jsonPath("$.exitCode").value("COMPLETED"))
                .andExpect(jsonPath("$.createdTaskCount").value(3))
                .andExpect(jsonPath("$.processedTaskCount").value(2))
                .andExpect(jsonPath("$.failedTaskCount").value(1));
        assertThat(runner.requests()).containsExactly(new DispatchRequest(11L, 12L, 9));
    }

    @Test
    void hidesRunnerFailureBehindInternalServerError() throws Exception {
        runner.failure = new IllegalStateException("내부 오류");

        mockMvc.perform(post("/internal/loadtest/v1/dispatch")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"issueId":11,"topicId":12,"preferredSendHour":9}
                                """))
                .andExpect(status().isInternalServerError());
    }

    @ParameterizedTest
    @MethodSource("invalidRequests")
    void rejectsInvalidRequestAtMvcBoundary(String request) throws Exception {
        mockMvc.perform(post("/internal/loadtest/v1/dispatch")
                        .contentType(APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isBadRequest());
    }

    private static Stream<String> invalidRequests() {
        return Stream.of(
                "{\"issueId\":0,\"topicId\":12,\"preferredSendHour\":9}",
                "{\"issueId\":null,\"topicId\":12,\"preferredSendHour\":9}",
                "{\"issueId\":11,\"topicId\":0,\"preferredSendHour\":9}",
                "{\"issueId\":11,\"topicId\":null,\"preferredSendHour\":9}",
                "{\"issueId\":11,\"topicId\":12,\"preferredSendHour\":-1}",
                "{\"issueId\":11,\"topicId\":12,\"preferredSendHour\":24}");
    }

    @Test
    void isExposedOnlyForLoadtestWithoutTestProfile() {
        assertThat(LoadtestDispatchController.class.getAnnotation(Profile.class).value())
                .containsExactly("loadtest & !test");
        assertThat(LoadtestDispatchController.class.getAnnotation(RequestMapping.class).value())
                .containsExactly("/internal/loadtest/v1/dispatch");
    }

    @Test
    void isNotCreatedForLocalProfile() {
        contextRunner.withPropertyValues("spring.profiles.active=local").run(context ->
                assertThat(context.getBeansOfType(LoadtestDispatchController.class)).isEmpty());
    }

    @Test
    void isCreatedForLoadtestProfileOnly() {
        contextRunner.withPropertyValues("spring.profiles.active=loadtest").run(context ->
                assertThat(context.getBeansOfType(LoadtestDispatchController.class)).hasSize(1));
    }

    @Test
    void isNotCreatedWhenTestProfileIsAlsoActive() {
        contextRunner.withPropertyValues("spring.profiles.active=loadtest,test").run(context ->
                assertThat(context.getBeansOfType(LoadtestDispatchController.class)).isEmpty());
    }

    @TestConfiguration(proxyBeanMethods = false)
    @Import(LoadtestDispatchController.class)
    static class LoadtestControllerConfiguration {

        @Bean
        DispatchJobRunner dispatchJobRunner() {
            return (issueId, topicId, preferredSendHour) ->
                    new DispatchJobRunSummary(1L, "COMPLETED", "COMPLETED", 0, 0, 0);
        }
    }

    private record DispatchRequest(Long issueId, Long topicId, int preferredSendHour) {
    }

    private static final class RecordingDispatchJobRunner implements DispatchJobRunner {

        private final List<DispatchRequest> requests = new ArrayList<>();
        private Exception failure;

        @Override
        public DispatchJobRunSummary run(Long issueId, Long topicId, int preferredSendHour) throws Exception {
            if (failure != null) {
                throw failure;
            }
            requests.add(new DispatchRequest(issueId, topicId, preferredSendHour));
            return new DispatchJobRunSummary(42L, "COMPLETED", "COMPLETED", 3, 2, 1);
        }

        private List<DispatchRequest> requests() {
            return requests;
        }
    }
}
