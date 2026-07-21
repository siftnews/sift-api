package com.siftnews.source.application.port.out;

import java.time.Instant;

public interface UpdateSourcePort {

    void markCrawled(Long sourceId, Instant at);
}
