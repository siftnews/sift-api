package com.siftnews.common;

import lombok.experimental.UtilityClass;

import java.net.URI;

@UtilityClass
public class UriNormalizer {

    public static String normalize(String url) {
        URI uri = parseUri(url);
        String scheme = uri.getScheme();
        String host = uri.getHost();

        return scheme.toLowerCase()
                + "://"
                + host.toLowerCase()
                + formatPort(scheme, uri.getPort())
                + formatPath(uri);
    }

    private static URI parseUri(String url) {
        if (url == null || url.isBlank()) {
            throw new IllegalArgumentException("url은 비어 있을 수 없습니다.");
        }

        URI uri;
        try {
            uri = URI.create(url.trim());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("유효하지 않은 url 형식입니다: " + url, e);
        }

        if (uri.getScheme() == null || uri.getHost() == null) {
            throw new IllegalArgumentException("유효한 http(s) url이 아닙니다: " + url);
        }
        return uri;
    }

    private static String formatPort(String scheme, int port) {
        if (port == -1 || isDefaultPort(scheme, port)) {
            return "";
        }
        return ":" + port;
    }

    private static boolean isDefaultPort(String scheme, int port) {
        return (scheme.equalsIgnoreCase("http") && port == 80)
                || (scheme.equalsIgnoreCase("https") && port == 443);
    }

    private static String formatPath(URI uri) {
        String path = uri.getRawPath();
        return path == null || path.isBlank() ? "/" : path;
    }
}
