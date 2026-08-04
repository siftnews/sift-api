package com.siftnews.source.adapter.in.bootstrap;

import com.siftnews.source.application.port.in.RenormalizeArticleUrlsUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * 기동 이벤트를 받아 저장된 기사 url을 현재 정규화 규칙으로 맞추는 <b>인바운드 어댑터</b> (이슈 #37).
 * <p>
 * 정규화 규칙을 «쿼리 전량 제거»에서 «추적 파라미터만 제거»로 바꾸면서 옛 키가 무효해졌다 —
 * 그대로 두면 같은 기사를 다시 수집할 때 {@code UNIQUE(normalized_url)}가 옛 키의 행을 찾지
 * 못해 중복 행이 생긴다. 실제로 값이 바뀌는 소스는 쿼리로 기사를 구분하는 AI타임스뿐이고,
 * 그 소스는 결함 때문에 한 행만 적재돼 있었다(2026-08-01 실측).
 * <p>
 * <b>수집 배치보다 먼저 돌아야 한다</b> — 순서가 뒤집히면 배치가 먼저 새 키로 적재해, 뒤이은
 * 재정규화가 {@code conflicted}로 밀리고 같은 기사가 두 행으로 남는다. Spring Batch의
 * {@code JobLauncherApplicationRunner}가 order {@code 0}이라 그보다 앞에 세운다
 * ({@link SourceSeeder} 다음 — 시드는 수집의 전제라 가장 먼저다).
 * <p>
 * <b>Liquibase를 도입하면 이 러너는 마이그레이션으로 옮기고 지운다</b>(MVP-DESIGN §2) — 매
 * 기동마다 전 행을 훑을 이유가 없다. 지금 러너인 것은 마이그레이션 인프라가 아직 없어서다.
 * {@code test} 프로파일에서는 동작하지 않는다({@code @Profile("!test")}) — 통합 테스트는
 * 유스케이스를 직접 부른다.
 */
@Component
@Profile("!test")
@Order(Ordered.HIGHEST_PRECEDENCE + 1)
@RequiredArgsConstructor
class ArticleUrlRenormalizer implements ApplicationRunner {

    private final RenormalizeArticleUrlsUseCase renormalizeArticleUrlsUseCase;

    @Override
    public void run(ApplicationArguments args) {
        renormalizeArticleUrlsUseCase.renormalize();
    }
}
