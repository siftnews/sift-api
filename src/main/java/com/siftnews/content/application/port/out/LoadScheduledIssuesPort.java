package com.siftnews.content.application.port.out;

import com.siftnews.content.api.ScheduledIssueReference;

import java.time.LocalDate;
import java.util.List;

public interface LoadScheduledIssuesPort {

    List<ScheduledIssueReference> loadScheduled(LocalDate runDate);
}
