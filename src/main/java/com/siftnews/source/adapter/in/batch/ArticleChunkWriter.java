package com.siftnews.source.adapter.in.batch;

import com.siftnews.source.application.port.out.SaveArticlePort;
import com.siftnews.source.domain.Article;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ItemWriter;

import java.util.List;

/**
 * collectStep writer — chunk(소스별 {@code List<Article>})를 평탄화해 한 번의
 * {@link SaveArticlePort#saveNew}로 저장한다. UNIQUE(normalized_url)로 중복은 DB에서 무시된다.
 */
@Slf4j
class ArticleChunkWriter implements ItemWriter<List<Article>> {

    private final SaveArticlePort saveArticlePort;

    ArticleChunkWriter(SaveArticlePort saveArticlePort) {
        this.saveArticlePort = saveArticlePort;
    }

    @Override
    public void write(Chunk<? extends List<Article>> chunk) {
        List<Article> articles = chunk.getItems().stream()
                .flatMap(List::stream)
                .toList();
        if (articles.isEmpty()) {
            return;
        }
        int savedCount = saveArticlePort.saveNew(articles);
        log.info("collectStep chunk 저장: 후보={}, 신규저장={}", articles.size(), savedCount);
    }
}
