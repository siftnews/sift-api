package com.siftnews.content.api;

import java.util.List;

public record NewsletterIssue(
        Long issueId,
        String title,
        List<NewsletterArticle> articles
) {
}
