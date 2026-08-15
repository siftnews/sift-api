package com.siftnews.source.adapter.in.bootstrap;

import com.siftnews.source.domain.Category;
import com.siftnews.source.domain.Source;
import com.siftnews.source.domain.SourceType;

import java.util.List;
import java.util.stream.IntStream;

final class LoadtestSourceSeedData {

    private LoadtestSourceSeedData() {
    }

    static List<Source> sources(String urlPrefix, int sourceCount) {
        return IntStream.range(0, sourceCount)
                .mapToObj(index -> Source.create(
                        "Loadtest source " + format(index),
                        SourceType.RSS,
                        urlPrefix + "source-" + format(index) + ".xml",
                        "en",
                        Category.DEV,
                        true))
                .toList();
    }

    private static String format(int index) {
        return "%02d".formatted(index);
    }
}
