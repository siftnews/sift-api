package com.siftnews.source.application.service;

import com.siftnews.common.UriNormalizer;
import com.siftnews.source.application.port.in.RenormalizeArticleUrlsUseCase;
import com.siftnews.source.application.port.in.RenormalizeSummary;
import com.siftnews.source.application.port.out.ArticleUrlPort;
import com.siftnews.source.application.port.out.StoredArticleUrl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class RenormalizeArticleUrlsService implements RenormalizeArticleUrlsUseCase {

    /** 커서 페이징 크기 — 전 행을 한 번에 싣지 않기 위한 값이라 크기 자체에 의미는 없다. */
    private static final int PAGE_SIZE = 500;

    private final ArticleUrlPort articleUrlPort;

    /**
     * id 오름차순 커서로 전 행을 훑는다. 갱신은 <b>건별 트랜잭션</b>이라(어댑터 경계) 한 건의
     * 충돌이 앞서 옮긴 행들을 되돌리지 않는다.
     * <p>
     * 커서가 offset이 아닌 이유: 갱신은 id를 바꾸지 않으므로 커서는 흔들리지 않는다. offset이면
     * 동시에 수집이 돌아 행이 늘어날 때 건너뛰는 행이 생긴다.
     */
    @Override
    public RenormalizeSummary renormalize() {
        int scanned = 0;
        int changed = 0;
        int unchanged = 0;
        int conflicted = 0;
        int invalid = 0;

        long cursor = 0L;
        while (true) {
            List<StoredArticleUrl> page = articleUrlPort.findUrlsAfter(cursor, PAGE_SIZE);
            if (page.isEmpty()) {
                break;
            }

            for (StoredArticleUrl stored : page) {
                scanned++;
                cursor = Math.max(cursor, stored.articleId());

                String renormalized;
                try {
                    renormalized = UriNormalizer.normalize(stored.url());
                } catch (IllegalArgumentException e) {
                    invalid++;
                    log.warn("재정규화 건너뜀 — url이 유효하지 않다: articleId={}, url={}",
                            stored.articleId(), stored.url());
                    continue;
                }

                if (renormalized.equals(stored.normalizedUrl())) {
                    unchanged++;
                } else if (articleUrlPort.updateNormalizedUrl(stored.articleId(), renormalized)) {
                    changed++;
                } else {
                    conflicted++;
                    log.warn("재정규화 건너뜀 — 새 키를 이미 다른 행이 쓰고 있다(같은 기사가 두 행): "
                            + "articleId={}, url={}, 새 키={}", stored.articleId(), stored.url(), renormalized);
                }
            }
        }

        RenormalizeSummary summary = new RenormalizeSummary(scanned, changed, unchanged, conflicted, invalid);
        log.info("기사 url 재정규화 완료: 스캔 {}건 · 갱신 {}건 · 유지 {}건 · 충돌 {}건 · 무효 {}건",
                scanned, changed, unchanged, conflicted, invalid);
        return summary;
    }
}
