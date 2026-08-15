package com.siftnews.content.application.port.in;

import java.time.Instant;
import java.time.LocalDate;

public interface SelectionJobRunner {

    SelectionJobRunSummary run(Long topicId, LocalDate runDate, Instant from, Instant to) throws Exception;
}
