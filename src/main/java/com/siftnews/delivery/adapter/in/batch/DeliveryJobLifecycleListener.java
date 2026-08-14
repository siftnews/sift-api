package com.siftnews.delivery.adapter.in.batch;

import com.siftnews.delivery.application.port.out.LoadDeliveryJobPort;
import com.siftnews.delivery.application.port.out.UpdateDeliveryJobPort;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobExecutionListener;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.StepExecutionListener;
import org.springframework.stereotype.Component;

@Component
final class DeliveryJobLifecycleListener implements StepExecutionListener, JobExecutionListener {

    private final LoadDeliveryJobPort loadDeliveryJobPort;
    private final UpdateDeliveryJobPort updateDeliveryJobPort;

    DeliveryJobLifecycleListener(LoadDeliveryJobPort loadDeliveryJobPort,
                                 UpdateDeliveryJobPort updateDeliveryJobPort) {
        this.loadDeliveryJobPort = loadDeliveryJobPort;
        this.updateDeliveryJobPort = updateDeliveryJobPort;
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
        Long deliveryJobId = loadDeliveryJobPort.loadByIssueId(issueId)
                .orElseThrow(() -> new IllegalStateException("발송 작업을 찾을 수 없습니다: issueId=" + issueId))
                .getDeliveryJobId();
        updateDeliveryJobPort.markSending(deliveryJobId);
    }

    @Override
    public void afterJob(JobExecution jobExecution) {
        updateDeliveryJobPort.markCompletedJobs();
    }
}
