package com.siftnews.source.adapter.in.batch;

import com.siftnews.source.application.port.out.LoadActiveSourcesPort;
import com.siftnews.source.domain.Source;
import org.springframework.batch.item.ItemReader;

import java.util.Iterator;

/**
 * collectStep reader — 활성 소스 목록을 한 번 적재해 아이템으로 순회한다.
 * <p>
 * {@code @StepScope}(빈 정의는 {@link CollectionJobConfig})로 매 Step 실행마다 새 인스턴스가
 * 생성되므로, 실행 간 상태(iterator)가 섞이지 않는다. 목록 적재는 최초 {@link #read()} 시
 * 한 번만 수행한다.
 */
class ActiveSourceItemReader implements ItemReader<Source> {

    private final LoadActiveSourcesPort loadActiveSourcesPort;
    private Iterator<Source> iterator;

    ActiveSourceItemReader(LoadActiveSourcesPort loadActiveSourcesPort) {
        this.loadActiveSourcesPort = loadActiveSourcesPort;
    }

    @Override
    public Source read() {
        if (iterator == null) {
            iterator = loadActiveSourcesPort.loadActive().iterator();
        }
        return iterator.hasNext() ? iterator.next() : null;
    }
}
