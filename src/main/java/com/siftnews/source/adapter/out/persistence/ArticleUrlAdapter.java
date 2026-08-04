package com.siftnews.source.adapter.out.persistence;

import com.siftnews.source.application.port.out.ArticleUrlPort;
import com.siftnews.source.application.port.out.StoredArticleUrl;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Component
@RequiredArgsConstructor
class ArticleUrlAdapter implements ArticleUrlPort {

    private final ArticleJpaRepository articleJpaRepository;

    @Override
    public List<StoredArticleUrl> findUrlsAfter(long afterId, int limit) {
        return articleJpaRepository.findByIdGreaterThanOrderByIdAsc(afterId, PageRequest.of(0, limit)).stream()
                .map(row -> new StoredArticleUrl(row.getId(), row.getUrl(), row.getNormalizedUrl()))
                .toList();
    }

    /**
     * 건별 트랜잭션이라 한 건의 충돌이 앞서 옮긴 행을 되돌리지 않는다.
     * <p>
     * 점유 여부를 미리 조회해 거르는 이유: {@code UNIQUE} 위반 예외로 판정하면 그 트랜잭션이
     * rollback-only가 되어 {@code false} 반환으로는 수습할 수 없다. 재정규화는 기동 시점에
     * 단독으로 도는 일회성 작업이라 조회와 갱신 사이에 끼어드는 쓰기가 없다.
     */
    @Override
    @Transactional
    public boolean updateNormalizedUrl(long articleId, String normalizedUrl) {
        if (articleJpaRepository.existsByNormalizedUrl(normalizedUrl)) {
            return false;
        }

        articleJpaRepository.updateNormalizedUrl(articleId, normalizedUrl);
        return true;
    }
}
