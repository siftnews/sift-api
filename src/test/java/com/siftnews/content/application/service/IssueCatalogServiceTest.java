package com.siftnews.content.application.service;

import com.siftnews.content.api.IssueCatalog;
import com.siftnews.content.application.port.out.LoadNewsletterIssuePort;
import com.siftnews.content.application.port.out.NewsletterIssueData;
import com.siftnews.content.application.port.out.NewsletterIssueItemData;
import com.siftnews.content.domain.ContentException;
import com.siftnews.source.api.ArticleCatalog;
import com.siftnews.source.api.ArticleCandidate;
import com.siftnews.source.api.ArticleExcerpt;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class IssueCatalogServiceTest {

    @Test
    void usesContentExceptionWhenIssueArticleIsMissing() {
        LoadNewsletterIssuePort issues = issueId -> Optional.of(new NewsletterIssueData(
                issueId, "Sift", List.of(new NewsletterIssueItemData(404L, 1))));
        ArticleCatalog articles = new ArticleCatalog() {
            @Override
            public List<ArticleCandidate> findCandidates(Instant from, Instant to) {
                return List.of();
            }

            @Override
            public void updateDedupClusters(Map<Long, String> clusterIdsByArticleId) {
            }

            @Override
            public List<ArticleExcerpt> findByIds(List<Long> articleIds) {
                return List.of();
            }
        };
        IssueCatalog service = new IssueCatalogService(
                runDate -> List.of(), issues, articles);

        assertThatThrownBy(() -> service.findNewsletterIssue(1L))
                .isInstanceOf(ContentException.class)
                .hasMessage("이슈 기사 참조 무결성 오류: articleId=404");
    }

    @Test
    void repeatsArticleLookupForEachNewsletterIssueLookup() {
        List<List<Long>> requestedArticleIds = new ArrayList<>();
        LoadNewsletterIssuePort issues = issueId -> Optional.of(new NewsletterIssueData(
                issueId, "Sift", List.of(new NewsletterIssueItemData(404L, 1))));
        ArticleCatalog articles = new ArticleCatalog() {
            @Override
            public List<ArticleCandidate> findCandidates(Instant from, Instant to) {
                return List.of();
            }

            @Override
            public void updateDedupClusters(Map<Long, String> clusterIdsByArticleId) {
            }

            @Override
            public List<ArticleExcerpt> findByIds(List<Long> articleIds) {
                requestedArticleIds.add(articleIds);
                return List.of(new ArticleExcerpt(404L, "기사", "https://example.com/article"));
            }
        };
        IssueCatalog service = new IssueCatalogService(runDate -> List.of(), issues, articles);

        service.findNewsletterIssue(1L);
        service.findNewsletterIssue(1L);

        assertThat(requestedArticleIds).containsExactly(List.of(404L), List.of(404L));
    }
}
