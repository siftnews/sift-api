package com.siftnews.source.application.port.in;

/**
 * 저장된 원본 {@code url}로 {@code normalized_url}을 다시 계산해 현재 정규화 규칙에 맞춘다 (이슈 #37).
 * <p>
 * <b>정규화 규칙이 바뀌면 기존 키가 무효해진다</b> — 같은 기사를 다시 수집해도
 * {@code UNIQUE(normalized_url)}가 옛 키의 행을 찾지 못해 중복 행으로 적재된다. 규칙을 바꾼
 * 배포와 함께 한 번 돌려 옛 키를 정리하는 것이 이 유스케이스의 존재 이유다.
 * <p>
 * <b>멱등하다</b> — 이미 규칙에 맞는 행은 건드리지 않으므로, 두 번째 실행부터는 전부
 * {@code unchanged}로 집계된다. 매 기동마다 돌아도 결과가 덧나지 않는다.
 */
public interface RenormalizeArticleUrlsUseCase {

    RenormalizeSummary renormalize();
}
