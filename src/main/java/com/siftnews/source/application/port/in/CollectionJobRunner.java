package com.siftnews.source.application.port.in;

public interface CollectionJobRunner {

    CollectionJobRunSummary run();

    CollectionJobRunSummary run(String runId);
}
