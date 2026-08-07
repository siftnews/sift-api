package com.siftnews.delivery.adapter.in.batch;

import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersInvalidException;
import org.springframework.batch.core.JobParametersValidator;

final class DispatchJobParametersValidator implements JobParametersValidator {

    @Override
    public void validate(JobParameters parameters) throws JobParametersInvalidException {
        require(parameters, DispatchJobParameters.ISSUE_ID);
        require(parameters, DispatchJobParameters.TOPIC_ID);

        Long sendHour = parameters.getLong(DispatchJobParameters.SEND_HOUR);
        if (sendHour == null || sendHour < 0 || sendHour > 23) {
            throw new JobParametersInvalidException(DispatchJobParameters.SEND_HOUR + "는 0부터 23 사이여야 합니다.");
        }
    }

    private static void require(JobParameters parameters, String name) throws JobParametersInvalidException {
        if (parameters.getLong(name) == null) {
            throw new JobParametersInvalidException(name + "는 필수입니다.");
        }
    }
}
