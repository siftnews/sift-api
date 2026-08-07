package com.siftnews.delivery.application.port.in;

public interface DispatchIssueUseCase {

    DispatchIssueSummary dispatch(Long issueId, Long topicId, int preferredSendHour);
}
