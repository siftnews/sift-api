package com.siftnews.source.adapter.out.persistence;

import com.siftnews.source.domain.Source;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * 기동 시 MVP 수집 소스 9종을 시드한다 — url 기준 멱등(이미 있으면 건너뜀).
 * <p>
 * {@code test} 프로파일에서는 동작하지 않는다({@code @Profile("!test")}) — 통합 테스트는
 * 자기 데이터를 직접 준비한다(CLAUDE.md). Liquibase 마이그레이션 전환은 후속(MVP-DESIGN §2).
 */
@Component
@Profile("!test")
class SourceSeeder implements ApplicationRunner {

    private final SourceJpaRepository sourceJpaRepository;

    SourceSeeder(SourceJpaRepository sourceJpaRepository) {
        this.sourceJpaRepository = sourceJpaRepository;
    }

    @Override
    public void run(ApplicationArguments args) {
        for (Source source : SourceSeedData.sources()) {
            if (!sourceJpaRepository.existsByUrl(source.getUrl())) {
                sourceJpaRepository.save(toEntity(source));
            }
        }
    }

    private static SourceJpaEntity toEntity(Source source) {
        return new SourceJpaEntity(source.getName(), source.getType(), source.getUrl(),
                source.getLang(), source.getCategory(), source.isActive(), source.getLastCrawledAt());
    }
}
