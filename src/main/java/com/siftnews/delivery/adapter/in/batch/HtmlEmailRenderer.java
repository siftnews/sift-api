package com.siftnews.delivery.adapter.in.batch;

import com.siftnews.content.api.NewsletterIssue;

final class HtmlEmailRenderer {

    String render(NewsletterIssue issue) {
        StringBuilder html = new StringBuilder("<html><body><h1>")
                .append(escape(issue.title()))
                .append("</h1><ol>");
        for (var article : issue.articles()) {
            html.append("<li><a href=\"")
                    .append(escape(article.url()))
                    .append("\">")
                    .append(escape(article.title()))
                    .append("</a></li>");
        }
        return html.append("</ol></body></html>").toString();
    }

    private static String escape(String value) {
        return value.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }
}
