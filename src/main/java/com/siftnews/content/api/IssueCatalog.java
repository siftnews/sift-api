package com.siftnews.content.api;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface IssueCatalog {

    List<ScheduledIssueReference> findScheduled(LocalDate runDate);

    Optional<NewsletterIssue> findNewsletterIssue(Long issueId);
}
