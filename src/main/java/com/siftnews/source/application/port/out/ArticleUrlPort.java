package com.siftnews.source.application.port.out;

import java.util.List;

/**
 * 재정규화가 쓰는 url 전용 조회·갱신 창구 (이슈 #37).
 * <p>
 * 기사 전체를 애그리거트로 싣지 않고 url 두 개만 읽는다 — 본문까지 끌어오면 전 행 스캔에
 * 불필요한 메모리를 쓴다.
 */
public interface ArticleUrlPort {

    /** {@code articleId > afterId}인 행을 id 오름차순으로 최대 {@code limit}건 — 커서 페이징. */
    List<StoredArticleUrl> findUrlsAfter(long afterId, int limit);

    /**
     * 한 행의 {@code normalized_url}을 바꾼다.
     *
     * @return 갱신하면 true, <b>다른 행이 이미 그 키를 점유해</b> 갱신하지 않았으면 false
     */
    boolean updateNormalizedUrl(long articleId, String normalizedUrl);
}
