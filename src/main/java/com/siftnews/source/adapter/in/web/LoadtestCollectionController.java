package com.siftnews.source.adapter.in.web;

import com.siftnews.source.adapter.in.web.dto.LoadtestCollectionResponse;
import com.siftnews.source.adapter.in.web.dto.LoadtestCollectionRequest;
import com.siftnews.source.application.port.in.CollectionJobRunner;
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
@RequestMapping("/internal/loadtest/v1/collection")
public class LoadtestCollectionController {

    private final CollectionJobRunner collectionJobRunner;

    public LoadtestCollectionController(CollectionJobRunner collectionJobRunner) {
        this.collectionJobRunner = collectionJobRunner;
    }

    @PostMapping
    public ResponseEntity<LoadtestCollectionResponse> collect(
            @Valid @RequestBody LoadtestCollectionRequest request) {
        try {
            return ResponseEntity.ok(LoadtestCollectionResponse.from(collectionJobRunner.run(request.runId())));
        } catch (Exception exception) {
            log.error("loadtest collectionJob 실행 실패: runId={}", request.runId(), exception);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}
