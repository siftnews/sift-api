package com.siftnews.delivery.adapter.in.web;

import com.siftnews.delivery.adapter.in.web.dto.LoadtestDispatchRequest;
import com.siftnews.delivery.adapter.in.web.dto.LoadtestDispatchResponse;
import com.siftnews.delivery.application.port.in.DispatchJobRunner;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@Profile("loadtest & !test")
@RequestMapping("/internal/loadtest/v1/dispatch")
public class LoadtestDispatchController {

    private final DispatchJobRunner dispatchJobRunner;

    public LoadtestDispatchController(DispatchJobRunner dispatchJobRunner) {
        this.dispatchJobRunner = dispatchJobRunner;
    }

    @PostMapping
    public ResponseEntity<LoadtestDispatchResponse> dispatch(
            @Valid @RequestBody LoadtestDispatchRequest request) {
        try {
            var summary = dispatchJobRunner.run(request.issueId(), request.topicId(),
                    request.preferredSendHour());
            return ResponseEntity.ok(LoadtestDispatchResponse.from(request, summary));
        } catch (Exception exception) {
            log.error("loadtest dispatchJob 실행 실패: issueId={} topicId={}", request.issueId(), request.topicId(),
                    exception);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}
