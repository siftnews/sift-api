package com.siftnews.source.adapter.in.bootstrap;

import com.siftnews.source.application.port.in.SeedSourcesUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * loadtest 프로파일에서만 synthetic source catalog를 준비한다.
 * <p>
 * 운영 소스는 삭제하지 않고 {@code LoadtestActiveSourcesAdapter}가 synthetic URL prefix만 읽도록
 * 분리한다. 따라서 같은 staging DB에서 운영 데이터와 loadtest fixture를 함께 보존할 수 있다.
 */
@Component
@Profile("loadtest & !test")
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
@RequiredArgsConstructor
class LoadtestSourceSeeder implements ApplicationRunner {

    private final SeedSourcesUseCase seedSourcesUseCase;

    @Value("${sift.load-test.news.source-url-prefix:https://loadtest.sift.local/news/}")
    private String sourceUrlPrefix;

    @Value("${sift.load-test.news.source-count:10}")
    private int sourceCount;

    @Override
    public void run(ApplicationArguments args) {
        seedSourcesUseCase.seed(LoadtestSourceSeedData.sources(sourceUrlPrefix, sourceCount));
    }
}
