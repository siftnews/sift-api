package com.siftnews.delivery.adapter.in.batch;

import com.siftnews.delivery.application.port.in.CompleteDeliveryJobsUseCase;
import com.siftnews.delivery.application.port.in.MarkDeliveryJobSendingUseCase;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobExecutionListener;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.StepExecutionListener;
import org.springframework.stereotype.Component;

@Component
final class DeliveryJobLifecycleListener implements StepExecutionListener, JobExecutionListener {

    private final MarkDeliveryJobSendingUseCase markDeliveryJobSendingUseCase;
    private final CompleteDeliveryJobsUseCase completeDeliveryJobsUseCase;

    DeliveryJobLifecycleListener(MarkDeliveryJobSendingUseCase markDeliveryJobSendingUseCase,
                                 CompleteDeliveryJobsUseCase completeDeliveryJobsUseCase) {
        this.markDeliveryJobSendingUseCase = markDeliveryJobSendingUseCase;
        this.completeDeliveryJobsUseCase = completeDeliveryJobsUseCase;
    }

    @Override
    public void beforeStep(StepExecution stepExecution) {
        if (!"sendStep".equals(stepExecution.getStepName())) {
            return;
        }
        Long issueId = stepExecution.getJobParameters().getLong(DispatchJobParameters.ISSUE_ID);
        if (issueId == null) {
            throw new IllegalStateException("발송 job의 issueId가 없습니다.");
        }
        markDeliveryJobSendingUseCase.markSending(issueId);
    }

    @Override
    public void afterJob(JobExecution jobExecution) {
        completeDeliveryJobsUseCase.complete();
    }
}
