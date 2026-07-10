package com.siftnews.source.application.port.out;

import com.siftnews.source.domain.Article;

import java.util.List;

public interface SaveArticlePort {

    int saveNew(List<Article> articles);
}
