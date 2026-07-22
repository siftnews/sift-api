package com.siftnews.content.adapter.out.persistence;

import com.siftnews.content.domain.Topic;

import java.util.List;
import java.util.Map;

/**
 * MVP 토픽 시드 정의 (MVP-DESIGN §1) — dev/ai/econ 3종.
 * <p>
 * 키워드 가중치는 기본 1.0(스코어링 시 적용)이라 빈 맵으로 두고, breakdown 로그를 보며
 * M2 스코어링 단계에서 튜닝한다. 도메인 {@link Topic#create}로 만들어 불변식을 함께 검증한다.
 */
final class TopicSeedData {

    private TopicSeedData() {
    }

    static List<Topic> topics() {
        return List.of(
                Topic.create("개발/엔지니어링", "dev", "ko,en",
                        List.of("Spring", "Kotlin", "Java", "Kubernetes", "백엔드", "아키텍처", "DevOps", "데이터베이스"),
                        List.of(), Map.of(), List.of("dev", "programming"),
                        24, 10, 0.0, true),
                Topic.create("AI/머신러닝", "ai", "ko,en",
                        List.of("LLM", "Claude", "GPT", "RAG", "에이전트", "파인튜닝", "트랜스포머", "추론"),
                        List.of(), Map.of(), List.of("ai", "ml"),
                        24, 10, 0.0, true),
                Topic.create("경제/금융/투자", "econ", "ko,en",
                        List.of("금리", "환율", "반도체", "인플레이션", "연준", "Fed", "코스피", "실적"),
                        List.of(), Map.of(), List.of("economy", "finance"),
                        24, 10, 0.0, true));
    }
}
