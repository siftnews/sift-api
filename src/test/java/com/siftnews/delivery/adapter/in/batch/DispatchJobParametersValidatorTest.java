package com.siftnews.delivery.adapter.in.batch;

import org.junit.jupiter.api.Test;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.JobParametersInvalidException;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DispatchJobParametersValidatorTest {

    private final DispatchJobParametersValidator validator = new DispatchJobParametersValidator();

    @Test
    void acceptsRequiredParametersAndValidSendHour() {
        assertThatCode(() -> validator.validate(new JobParametersBuilder()
                .addLong(DispatchJobParameters.ISSUE_ID, 1L)
                .addLong(DispatchJobParameters.TOPIC_ID, 2L)
                .addLong(DispatchJobParameters.SEND_HOUR, 23L)
                .toJobParameters()))
                .doesNotThrowAnyException();
    }

    @Test
    void rejectsMissingIssueId() {
        assertThatThrownBy(() -> validator.validate(new JobParametersBuilder()
                .addLong(DispatchJobParameters.TOPIC_ID, 2L)
                .addLong(DispatchJobParameters.SEND_HOUR, 9L)
                .toJobParameters()))
                .isInstanceOf(JobParametersInvalidException.class)
                .hasMessageContaining(DispatchJobParameters.ISSUE_ID);
    }

    @Test
    void rejectsOutOfRangeSendHour() {
        assertThatThrownBy(() -> validator.validate(new JobParametersBuilder()
                .addLong(DispatchJobParameters.ISSUE_ID, 1L)
                .addLong(DispatchJobParameters.TOPIC_ID, 2L)
                .addLong(DispatchJobParameters.SEND_HOUR, 24L)
                .toJobParameters()))
                .isInstanceOf(JobParametersInvalidException.class)
                .hasMessageContaining(DispatchJobParameters.SEND_HOUR);
    }
}
