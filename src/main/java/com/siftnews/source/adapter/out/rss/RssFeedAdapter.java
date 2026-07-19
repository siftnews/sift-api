package com.siftnews.source.adapter.out.rss;

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
                entry.getDescription().getValue(),
                source.getLang(),
                entry.getPublishedDate() != null ? entry.getPublishedDate().toInstant() : null,
                source.getCategory()
        );
    }
}
