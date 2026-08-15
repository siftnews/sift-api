package com.siftnews.source.application.port.in;

public interface CollectionJobRunner {

    CollectionJobRunSummary run() throws Exception;

    CollectionJobRunSummary run(String runId) throws Exception;
}
