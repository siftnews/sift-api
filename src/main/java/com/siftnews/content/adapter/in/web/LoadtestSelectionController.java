package com.siftnews.content.adapter.in.web;

import com.siftnews.content.adapter.in.web.dto.LoadtestSelectionRequest;
import com.siftnews.content.adapter.in.web.dto.LoadtestSelectionResponse;
import com.siftnews.content.application.port.in.SelectionJobRunner;
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
@RequestMapping("/internal/loadtest/v1/selection")
public class LoadtestSelectionController {

    private final SelectionJobRunner selectionJobRunner;

    public LoadtestSelectionController(SelectionJobRunner selectionJobRunner) {
        this.selectionJobRunner = selectionJobRunner;
    }

    @PostMapping
    public ResponseEntity<LoadtestSelectionResponse> select(
            @Valid @RequestBody LoadtestSelectionRequest request) {
        try {
            return ResponseEntity.ok(LoadtestSelectionResponse.from(selectionJobRunner.run(
                    request.topicId(), request.runDate(), request.from(), request.to())));
        } catch (Exception exception) {
            log.error("loadtest selectionJob 실행 실패: topicId={} runDate={}", request.topicId(), request.runDate(),
                    exception);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}
