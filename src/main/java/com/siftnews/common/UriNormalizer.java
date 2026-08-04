package com.siftnews.common;

import lombok.experimental.UtilityClass;

import java.net.URI;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

/**
 * 기사 url을 <b>동일성 판정 키</b>로 환산한다 — {@code UNIQUE(normalized_url)} 적재 키이자
 * {@code DedupClusterer}의 병합 기준 ①이라, 여기서 뭉개면 적재와 dedup이 함께 무너진다.
 * <p>
 * <b>쿼리는 추적 파라미터만 버리고 나머지는 남긴다.</b> 예전에는 쿼리를 통째로 버렸는데,
 * {@code ?idxno=213427}처럼 <b>쿼리로 기사를 구분하는 소스</b>(AI타임스 등 한국 언론사 다수)의
 * 기사가 전부 같은 값으로 환산돼 50건 중 1건만 적재됐다. 남기는 쿼리는 키 오름차순으로
 * 정렬해, 같은 기사가 파라미터 순서만 다르게 실려도 같은 키가 되게 한다.
 */
@UtilityClass
public class UriNormalizer {

    /**
     * 값이 달라도 같은 기사를 가리키는 파라미터들 — 유입 경로를 기록할 뿐 문서를 고르지 않는다.
     * 접두어로 판정하는 둘은 계열 전체가 추적용이기 때문이다: {@code utm_*}는 UTM 표준,
     * {@code at_*}는 BBC 계열({@code at_medium}·{@code at_campaign}·{@code at_bbc_team} …).
     */
    private static final List<String> TRACKING_PARAM_PREFIXES = List.of("utm_", "at_");
    private static final List<String> TRACKING_PARAM_NAMES =
            List.of("fbclid", "gclid", "igshid", "mc_cid", "mc_eid", "ref");

    public static String normalize(String url) {
        URI uri = parseUri(url);
        String scheme = uri.getScheme();
        String host = uri.getHost();

        return scheme.toLowerCase()
                + "://"
                + host.toLowerCase()
                + formatPort(scheme, uri.getPort())
                + formatPath(uri)
                + formatQuery(uri);
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

        String scheme = uri.getScheme();
        if (!scheme.equalsIgnoreCase("http") && !scheme.equalsIgnoreCase("https")) {
            throw new IllegalArgumentException("지원되지 않는 url 스킴입니다: " + scheme);
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

    /**
     * 인코딩된 원본({@code getRawQuery})을 그대로 다룬다 — 디코딩하면 값에 들어 있던
     * {@code &}·{@code =}가 구분자와 섞여 파라미터 경계가 무너진다.
     */
    private static String formatQuery(URI uri) {
        String rawQuery = uri.getRawQuery();
        if (rawQuery == null || rawQuery.isBlank()) {
            return "";
        }

        String kept = Arrays.stream(rawQuery.split("&"))
                .filter(param -> !param.isBlank())
                .filter(param -> !isTracking(nameOf(param)))
                .sorted(Comparator.comparing(UriNormalizer::nameOf).thenComparing(Comparator.naturalOrder()))
                .collect(Collectors.joining("&"));

        return kept.isEmpty() ? "" : "?" + kept;
    }

    /** {@code a=1} → {@code a}, 값이 없는 {@code a} → {@code a}. */
    private static String nameOf(String param) {
        int separator = param.indexOf('=');
        return separator < 0 ? param : param.substring(0, separator);
    }

    private static boolean isTracking(String name) {
        String lowered = name.toLowerCase(Locale.ROOT);
        return TRACKING_PARAM_PREFIXES.stream().anyMatch(lowered::startsWith)
                || TRACKING_PARAM_NAMES.contains(lowered);
    }
}
