package com.siftnews.source.adapter.in.batch;

import com.siftnews.source.application.port.out.SaveArticlePort;
import com.siftnews.source.domain.Article;
import com.siftnews.source.domain.Category;
import com.siftnews.source.domain.RawArticle;
import org.junit.jupiter.api.Test;
import org.springframework.batch.item.Chunk;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ArticleChunkWriterTest {

    private static final Instant NOW = Instant.parse("2026-07-10T00:00:00Z");

    @Test
    void flattensPerSourceArticleListsIntoOneSaveNewCall() throws Exception {
        FakeSaveArticlePort saveArticlePort = new FakeSaveArticlePort();
        ArticleChunkWriter writer = new ArticleChunkWriter(saveArticlePort);

        Chunk<List<Article>> chunk = new Chunk<>(List.of(
                List.of(article("https://a.example.com/1"), article("https://a.example.com/2")),
                List.of(article("https://b.example.com/1"))
        ));

        writer.write(chunk);

        assertThat(saveArticlePort.saveNewCallCount()).isEqualTo(1);
        assertThat(saveArticlePort.savedArticles())
                .extracting(Article::getUrl)
                .containsExactly("https://a.example.com/1", "https://a.example.com/2", "https://b.example.com/1");
    }

    @Test
    void skipsSaveNewWhenChunkHasNoArticles() throws Exception {
        FakeSaveArticlePort saveArticlePort = new FakeSaveArticlePort();
        ArticleChunkWriter writer = new ArticleChunkWriter(saveArticlePort);

        Chunk<List<Article>> chunk = new Chunk<>(List.of(List.of(), List.of()));

        writer.write(chunk);

        assertThat(saveArticlePort.saveNewCallCount()).isZero();
        assertThat(saveArticlePort.savedArticles()).isEmpty();
    }

    private static Article article(String url) {
        return Article.create(new RawArticle(url, "제목", "본문", "en", NOW, Category.DEV), 1L);
    }

    private static class FakeSaveArticlePort implements SaveArticlePort {
        private final List<Article> saved = new ArrayList<>();
        private int saveNewCallCount = 0;

        List<Article> savedArticles() {
            return saved;
        }

        int saveNewCallCount() {
            return saveNewCallCount;
        }

        @Override
        public int saveNew(List<Article> articles) {
            saveNewCallCount++;
            saved.addAll(articles);
            return articles.size();
        }
    }
}
