package com.siftnews.source.adapter.out.rss;

import com.siftnews.source.domain.Category;
import com.siftnews.source.domain.RawArticle;
import com.siftnews.source.domain.Source;
import com.siftnews.source.domain.SourceType;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RssFeedAdapterTest {

    private final RssFeedAdapter adapter = new RssFeedAdapter();

    @Test
    void parse는_RSS_2_0_피드를_RawArticle_목록으로_변환한다() {
        Source source = Source.restore(1L, "Test Source", SourceType.RSS,
                "https://example.com/rss", "en", Category.DEV, true, null);

        try (InputStream feedStream = getClass().getResourceAsStream("/rss/valid-feed.xml")) {
            List<RawArticle> articles = adapter.parse(source, feedStream);

            assertThat(articles).hasSize(3);

            RawArticle first = articles.get(0);
            assertThat(first.url()).isEqualTo("https://example.com/articles/1");
            assertThat(first.title()).isEqualTo("First Article");
            assertThat(first.body()).isEqualTo("Body of the first article.");
            assertThat(first.lang()).isEqualTo("en");
            assertThat(first.category()).isEqualTo(Category.DEV);
            assertThat(first.publishedAt()).isEqualTo(
                    ZonedDateTime.of(2026, 7, 15, 10, 0, 0, 0, ZoneOffset.UTC).toInstant());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    void parse는_아이템이_없는_피드에_대해_빈_목록을_반환한다() {
        Source source = Source.restore(1L, "Test Source", SourceType.RSS,
                "https://example.com/rss", "en", Category.DEV, true, null);

        try (InputStream feedStream = getClass().getResourceAsStream("/rss/empty-feed.xml")) {
            List<RawArticle> articles = adapter.parse(source, feedStream);

            assertThat(articles).isEmpty();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    void parse는_pubDate가_없는_항목의_publishedAt을_null로_설정한다() {
        Source source = Source.restore(1L, "Test Source", SourceType.RSS,
                "https://example.com/rss", "en", Category.DEV, true, null);

        try (InputStream feedStream = getClass().getResourceAsStream("/rss/missing-pubdate-feed.xml")) {
            List<RawArticle> articles = adapter.parse(source, feedStream);

            assertThat(articles).hasSize(2);
            assertThat(articles.get(0).publishedAt()).isNull();
            assertThat(articles.get(1).publishedAt()).isNotNull();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    void parse는_malformed_XML에_대해_예외를_던진다() {
        Source source = Source.restore(1L, "Test Source", SourceType.RSS,
                "https://example.com/rss", "en", Category.DEV, true, null);

        try (InputStream feedStream = getClass().getResourceAsStream("/rss/malformed-feed.xml")) {
            assertThatThrownBy(() -> adapter.parse(source, feedStream))
                    .isInstanceOf(Exception.class);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
