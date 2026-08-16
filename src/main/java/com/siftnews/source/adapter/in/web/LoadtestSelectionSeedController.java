package com.siftnews.source.adapter.in.web;

import com.siftnews.source.adapter.in.web.dto.LoadtestSelectionSeedRequest;
import com.siftnews.source.adapter.in.web.dto.LoadtestSelectionSeedResponse;
import com.siftnews.source.application.port.in.SeedLoadtestArticlesUseCase;
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
@RequestMapping("/internal/loadtest/v1/selection/seed")
public class LoadtestSelectionSeedController {

    private final SeedLoadtestArticlesUseCase seedLoadtestArticlesUseCase;

    public LoadtestSelectionSeedController(SeedLoadtestArticlesUseCase seedLoadtestArticlesUseCase) {
        this.seedLoadtestArticlesUseCase = seedLoadtestArticlesUseCase;
    }

    @PostMapping
    public ResponseEntity<LoadtestSelectionSeedResponse> seed(
            @Valid @RequestBody LoadtestSelectionSeedRequest request) {
        try {
            return ResponseEntity.ok(LoadtestSelectionSeedResponse.from(
                    seedLoadtestArticlesUseCase.seed(request.runId())));
        } catch (Exception exception) {
            log.error("loadtest selection fixture 시드 실패: runId={}", request.runId(), exception);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}
