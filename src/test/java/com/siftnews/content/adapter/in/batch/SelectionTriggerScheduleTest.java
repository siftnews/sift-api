package com.siftnews.content.adapter.in.batch;

import org.junit.jupiter.api.Test;
import org.springframework.scheduling.annotation.Scheduled;

import java.lang.reflect.Method;

import static org.assertj.core.api.Assertions.assertThat;

class SelectionTriggerScheduleTest {

    @Test
    void defaultsToMidnightInSeoul() throws NoSuchMethodException {
        Method method = SelectionTrigger.class.getDeclaredMethod("triggerDailySelection");
        Scheduled scheduled = method.getAnnotation(Scheduled.class);

        assertThat(scheduled.cron()).isEqualTo("${sift.selection.cron:0 0 0 * * *}");
        assertThat(scheduled.zone()).isEqualTo("${sift.selection.zone:Asia/Seoul}");
    }
}
