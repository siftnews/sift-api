package com.siftnews.content.application.port.out;

import java.util.List;

public record NewsletterIssueData(
        Long issueId,
        String title,
        List<NewsletterIssueItemData> items
) {
}
