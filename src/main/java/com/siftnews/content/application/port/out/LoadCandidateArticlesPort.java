package com.siftnews.content.application.port.out;

import com.siftnews.content.domain.CandidateArticle;

import java.util.List;

/**
 * 선별 후보 기사 조회 — 실 구현은 Source named interface를 호출하는 어댑터(D-018·D-030),
 * selectionJob 배치(M2-5)에서 배선한다.
 */
public interface LoadCandidateArticlesPort {

    List<CandidateArticle> loadCandidates();
}
