package com.siftnews.content.application.service;

import com.siftnews.content.api.IssueCatalog;
import com.siftnews.content.api.NewsletterArticle;
import com.siftnews.content.api.NewsletterIssue;
import com.siftnews.content.api.ScheduledIssueReference;
import com.siftnews.content.application.port.out.LoadNewsletterIssuePort;
import com.siftnews.content.application.port.out.LoadScheduledIssuesPort;
import com.siftnews.content.application.port.out.NewsletterIssueData;
import com.siftnews.content.application.port.out.NewsletterIssueItemData;
import com.siftnews.content.domain.ContentException;
import com.siftnews.source.api.ArticleCatalog;
import com.siftnews.source.api.ArticleExcerpt;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
class IssueCatalogService implements IssueCatalog {

    private final LoadScheduledIssuesPort loadScheduledIssuesPort;
    private final LoadNewsletterIssuePort loadNewsletterIssuePort;
    private final ArticleCatalog articleCatalog;

    @Override
    public List<ScheduledIssueReference> findScheduled(LocalDate runDate) {
        return loadScheduledIssuesPort.loadScheduled(runDate);
    }

    @Override
    public Optional<NewsletterIssue> findNewsletterIssue(Long issueId) {
        return loadNewsletterIssuePort.loadNewsletterIssue(issueId)
                .map(this::toNewsletterIssue);
    }

    private NewsletterIssue toNewsletterIssue(NewsletterIssueData data) {
        Map<Long, ArticleExcerpt> articlesById = articleCatalog.findByIds(data.items().stream()
                        .map(NewsletterIssueItemData::articleId)
                        .toList())
                .stream()
                .collect(Collectors.toMap(ArticleExcerpt::articleId, article -> article));
        List<NewsletterArticle> articles = data.items().stream()
                .map(item -> toNewsletterArticle(item, articlesById))
                .toList();
        return new NewsletterIssue(data.issueId(), data.title(), articles);
    }

    private NewsletterArticle toNewsletterArticle(NewsletterIssueItemData item,
                                                  Map<Long, ArticleExcerpt> articlesById) {
        ArticleExcerpt article = articlesById.get(item.articleId());
        if (article == null) {
            throw new ContentException("이슈 기사 참조 무결성 오류: articleId=" + item.articleId());
        }
        return new NewsletterArticle(item.rank(), article.title(), article.url());
    }
}
