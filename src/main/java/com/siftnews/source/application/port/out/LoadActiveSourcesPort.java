package com.siftnews.source.application.port.out;

import com.siftnews.source.domain.Source;

import java.util.List;

public interface LoadActiveSourcesPort {

    List<Source> loadActive();
}
