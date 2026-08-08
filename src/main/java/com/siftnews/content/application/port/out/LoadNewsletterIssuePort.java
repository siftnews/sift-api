package com.siftnews.content.application.port.out;

import java.util.Optional;

public interface LoadNewsletterIssuePort {

    Optional<NewsletterIssueData> loadNewsletterIssue(Long issueId);
}
