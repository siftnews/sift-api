package com.siftnews.source.application.port.out;

import com.siftnews.source.domain.RawArticle;
import com.siftnews.source.domain.Source;

import java.util.List;

public interface FetchFeedPort {

    List<RawArticle> fetch(Source source);
}
