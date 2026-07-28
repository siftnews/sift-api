package com.siftnews.source.application.service;

import com.siftnews.source.application.port.in.SeedSourcesUseCase;
import com.siftnews.source.application.port.out.SaveSourcePort;
import com.siftnews.source.domain.Source;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class SeedSourcesService implements SeedSourcesUseCase {

    private final SaveSourcePort saveSourcePort;

    @Override
    public int seed(List<Source> sources) {
        int inserted = saveSourcePort.saveNew(sources);
        log.info("소스 시드 완료: 요청 {}건 · 신규 {}건", sources.size(), inserted);
        return inserted;
    }
}
