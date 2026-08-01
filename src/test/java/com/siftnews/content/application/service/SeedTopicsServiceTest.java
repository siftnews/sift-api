package com.siftnews.content.application.service;

import com.siftnews.content.application.port.out.SaveTopicPort;
import com.siftnews.content.domain.Topic;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class SeedTopicsServiceTest {

    private final FakeSaveTopicPort saveTopicPort = new FakeSaveTopicPort();
    private final SeedTopicsService service = new SeedTopicsService(saveTopicPort);

    @Test
    void seedPassesCatalogToPortAndReturnsInsertedCount() {
        List<Topic> topics = List.of(topic("dev"), topic("ai"));
        saveTopicPort.willInsert(2);

        int inserted = service.seed(topics);

        assertThat(inserted).isEqualTo(2);
        assertThat(saveTopicPort.received).containsExactlyElementsOf(topics);
    }

    /** 이미 다 심긴 재기동은 0건이 정상이다 — 예외가 아니라 건수로 드러나야 한다. */
    @Test
    void seedReportsZeroWhenNothingIsNew() {
        saveTopicPort.willInsert(0);

        assertThat(service.seed(List.of(topic("dev")))).isZero();
    }

    private static Topic topic(String slug) {
        return Topic.create("이름-" + slug, slug, "ko,en", List.of("키워드"), List.of(), Map.of(),
                List.of("dev"), 24, 10, 0.0, true);
    }

    private static class FakeSaveTopicPort implements SaveTopicPort {

        private final List<Topic> received = new ArrayList<>();
        private int insertedCount;

        void willInsert(int count) {
            this.insertedCount = count;
        }

        @Override
        public int saveNew(List<Topic> topics) {
            received.addAll(topics);
            return insertedCount;
        }
    }
}
