package com.siftnews.delivery.application.service;

import com.siftnews.content.api.NewsletterIssue;

import java.net.URI;
import java.util.Locale;

final class HtmlEmailRenderer {

    String render(NewsletterIssue issue) {
        StringBuilder html = new StringBuilder("<html><body><h1>")
                .append(escape(issue.title()))
                .append("</h1><ol>");
        for (var article : issue.articles()) {
            html.append("<li>");
            if (isAllowedUrl(article.url())) {
                html.append("<a href=\"")
                        .append(escape(article.url()))
                        .append("\">")
                        .append(escape(article.title()))
                        .append("</a>");
            } else {
                html.append(escape(article.title()));
            }
            html.append("</li>");
        }
        return html.append("</ol></body></html>").toString();
    }

    private static boolean isAllowedUrl(String value) {
        if (value == null || value.isBlank()) {
            return false;
        }
        try {
            URI uri = URI.create(value);
            String scheme = uri.getScheme();
            return uri.isAbsolute() && scheme != null
                    && ("http".equals(scheme.toLowerCase(Locale.ROOT))
                    || "https".equals(scheme.toLowerCase(Locale.ROOT)));
        } catch (IllegalArgumentException exception) {
            return false;
        }
    }

    private static String escape(String value) {
        return value.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }
}
