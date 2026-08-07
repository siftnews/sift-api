package com.siftnews.content.application.service;

import com.siftnews.content.api.IssueCatalog;
import com.siftnews.content.api.ScheduledIssueReference;
import com.siftnews.content.application.port.out.LoadScheduledIssuesPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
class IssueCatalogService implements IssueCatalog {

    private final LoadScheduledIssuesPort loadScheduledIssuesPort;

    @Override
    public List<ScheduledIssueReference> findScheduled(LocalDate runDate) {
        return loadScheduledIssuesPort.loadScheduled(runDate);
    }
}
