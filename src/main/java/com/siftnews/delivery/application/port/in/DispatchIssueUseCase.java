package com.siftnews.delivery.application.port.in;

public interface DispatchIssueUseCase {

    Long dispatch(Long issueId, Long topicId, int preferredSendHour);
}
