package com.siftnews.source.application.port.out;

import com.siftnews.source.domain.Source;

import java.util.List;
import java.util.Optional;

public interface LoadActiveSourcesPort {

    List<Source> loadActive();

    Optional<Source> findActiveById(Long sourceId);
}
