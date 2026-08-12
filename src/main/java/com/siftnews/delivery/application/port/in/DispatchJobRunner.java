package com.siftnews.delivery.application.port.in;

public interface DispatchJobRunner {

    DispatchJobRunSummary run(Long issueId, Long topicId, int preferredSendHour) throws Exception;
}
