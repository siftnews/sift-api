package com.siftnews.content.application.service;

import com.siftnews.content.application.port.in.SeedTopicsUseCase;
import com.siftnews.content.application.port.out.SaveTopicPort;
import com.siftnews.content.domain.Topic;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class SeedTopicsService implements SeedTopicsUseCase {

    private final SaveTopicPort saveTopicPort;

    @Override
    public int seed(List<Topic> topics) {
        int inserted = saveTopicPort.saveNew(topics);
        log.info("토픽 시드 완료: 요청 {}건 · 신규 {}건", topics.size(), inserted);
        return inserted;
    }
}
