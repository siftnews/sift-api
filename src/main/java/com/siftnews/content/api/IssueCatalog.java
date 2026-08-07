package com.siftnews.content.api;

import java.time.LocalDate;
import java.util.List;

public interface IssueCatalog {

    List<ScheduledIssueReference> findScheduled(LocalDate runDate);
}
