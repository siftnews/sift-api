package com.siftnews.source.adapter.in.bootstrap;

import com.siftnews.source.domain.Category;
import com.siftnews.source.domain.Source;
import com.siftnews.source.domain.SourceType;

import java.util.List;

/**
 * MVP 소스 시드 정의 (MVP-DESIGN §1) — dev/ai/econ 토픽별 3종씩 9종(한국어 4 · 영어 5).
 * <p>
 * 모든 url은 2026-07-26에 실제 응답으로 검증했다(HTTP 200 + RSS 루트 태그 + 최근 갱신 확인).
 * 도메인 {@link Source#create}로 만들어 불변식을 함께 검증한다.
 * <p>
 * {@code category}는 토픽이 후보를 끌어올 때 쓰는 태그와 대응한다(TopicSeedData의
 * {@code sourceCategories}) — dev 토픽은 {@code dev·programming}, ai는 {@code ai·ml},
 * econ은 {@code economy·finance}. {@link Category#ML}을 쓰는 소스는 아직 없다(AI로 충분).
 * <p>
 * 제외한 후보: 우아한형제들·LINE 기술블로그·CNBC(403), Yahoo Finance(429), 연합뉴스 경제(연결 실패),
 * Anthropic(404 — 공식 RSS 미제공), 카카오테크(응답은 200이나 최신 글이 2026-06-23로 정체).
 * Hugging Face(831건)·OpenAI(1050건)는 전체 히스토리를 담은 피드라 첫 수집에 수백 건이 한꺼번에
 * 들어와 e2e 확인을 방해하므로 20건 규모 피드로 대체했다. 네이버 D2는 Atom 형식이라
 * {@code RssFeedAdapter}의 미검증 경로 — 후속 이슈로 분리.
 */
final class SourceSeedData {

    private SourceSeedData() {
    }

    static List<Source> sources() {
        return List.of(
                // dev 토픽
                Source.create("Hacker News", SourceType.RSS,
                        "https://news.ycombinator.com/rss", "en", Category.PROGRAMMING, true),
                Source.create("토스 기술블로그", SourceType.RSS,
                        "https://toss.tech/rss.xml", "ko", Category.DEV, true),
                Source.create("Ars Technica", SourceType.RSS,
                        "https://feeds.arstechnica.com/arstechnica/index", "en", Category.DEV, true),

                // ai 토픽
                Source.create("AI타임스", SourceType.RSS,
                        "https://www.aitimes.com/rss/allArticle.xml", "ko", Category.AI, true),
                Source.create("Import AI", SourceType.RSS,
                        "https://importai.substack.com/feed", "en", Category.AI, true),
                Source.create("Google AI Blog", SourceType.RSS,
                        "https://blog.google/technology/ai/rss/", "en", Category.AI, true),

                // econ 토픽
                Source.create("한국경제 경제", SourceType.RSS,
                        "https://www.hankyung.com/feed/economy", "ko", Category.ECONOMY, true),
                Source.create("매일경제 경제", SourceType.RSS,
                        "https://www.mk.co.kr/rss/30100041/", "ko", Category.ECONOMY, true),
                Source.create("BBC Business", SourceType.RSS,
                        "https://feeds.bbci.co.uk/news/business/rss.xml", "en", Category.FINANCE, true));
    }
}
