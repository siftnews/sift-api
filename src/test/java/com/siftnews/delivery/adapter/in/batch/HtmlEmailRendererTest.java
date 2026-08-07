package com.siftnews.delivery.adapter.in.batch;

import com.siftnews.content.api.NewsletterArticle;
import com.siftnews.content.api.NewsletterIssue;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class HtmlEmailRendererTest {

    private final HtmlEmailRenderer renderer = new HtmlEmailRenderer();

    @Test
    void escapesExternalValuesAndKeepsArticleOrder() {
        String html = renderer.render(new NewsletterIssue(1L, "Sift <뉴스> & '특집'", List.of(
                new NewsletterArticle(1, "첫 <기사>", "https://example.com?a=1&b=2"),
                new NewsletterArticle(2, "둘째 \"기사\"", "https://example.com/second"))));

        assertThat(html).contains("Sift &lt;뉴스&gt; &amp; &#39;특집&#39;");
        assertThat(html).contains("https://example.com?a=1&amp;b=2");
        assertThat(html).contains("첫 &lt;기사&gt;");
        assertThat(html).contains("둘째 &quot;기사&quot;");
        assertThat(html.indexOf("첫 &lt;기사&gt;")).isLessThan(html.indexOf("둘째 &quot;기사&quot;"));
    }
}
