package com.siftnews.source.adapter.out.rss;

import com.rometools.rome.feed.synd.SyndContent;
import com.rometools.rome.feed.synd.SyndEntry;
import com.rometools.rome.feed.synd.SyndFeed;
import com.rometools.rome.io.SyndFeedInput;
import com.rometools.rome.io.XmlReader;
import com.siftnews.source.application.port.out.FetchFeedPort;
import com.siftnews.source.domain.RawArticle;
import com.siftnews.source.domain.Source;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.net.URI;
import java.util.List;
import java.util.Objects;

@Component
class RssFeedAdapter implements FetchFeedPort {

    @Override
    public List<RawArticle> fetch(Source source) {
        try (InputStream feedStream = URI.create(source.getUrl()).toURL().openStream()) {
            return parse(source, feedStream);
        } catch (Exception e) {
            throw new RuntimeException("RSS 피드 조회 실패: " + source.getUrl(), e);
        }
    }

    List<RawArticle> parse(Source source, InputStream feedStream) throws Exception {
        SyndFeedInput input = new SyndFeedInput();
        SyndFeed feed = input.build(new XmlReader(feedStream));

        return feed.getEntries().stream()
                .map(entry -> toRawArticle(source, entry))
                .toList();
    }

    private RawArticle toRawArticle(Source source, SyndEntry entry) {
        return new RawArticle(
                entry.getLink(),
                entry.getTitle(),
                descriptionOf(entry),
                source.getLang(),
                entry.getPublishedDate() != null ? entry.getPublishedDate().toInstant() : null,
                source.getCategory()
        );
    }

    /**
     * {@code <description>}이 없는 피드가 있다 — 한국경제는 title·link·author·pubDate만 싣고,
     * 토스는 일부 항목이 {@code <content:encoded>}만 갖는다. 방어가 없으면 NPE가 나면서
     * <b>그 소스의 수집이 통째로 실패</b>한다(2026-07-26 e2e에서 9개 중 2개 소스가 이 경로로 누락).
     */
    private static String descriptionOf(SyndEntry entry) {
        if (entry.getDescription() != null) {
            return entry.getDescription().getValue();
        }
        return entry.getContents().stream()
                .map(SyndContent::getValue)
                .filter(Objects::nonNull)
                .findFirst()
                .orElse(null);
    }
}
