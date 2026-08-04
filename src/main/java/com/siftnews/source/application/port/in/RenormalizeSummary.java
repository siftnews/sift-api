package com.siftnews.source.application.port.in;

/**
 * 재정규화 결과 집계 — 무엇이 옮겨졌고 무엇이 남았는지 기동 로그로 확인할 수 있어야 한다.
 *
 * @param scanned    훑은 행 수
 * @param changed    새 키로 갱신한 행 수
 * @param unchanged  이미 규칙에 맞아 건드리지 않은 행 수
 * @param conflicted 새 키를 이미 다른 행이 점유해 건너뛴 행 수 — <b>같은 기사가 두 행으로
 *                   존재한다는 뜻</b>이라, 사람이 확인해 정리할 수 있게 따로 센다
 * @param invalid    저장된 url이 정규화 불가라 건너뛴 행 수
 */
public record RenormalizeSummary(int scanned, int changed, int unchanged, int conflicted, int invalid) {

    public static RenormalizeSummary empty() {
        return new RenormalizeSummary(0, 0, 0, 0, 0);
    }
}
