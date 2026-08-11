package com.siftnews.subscriber.adapter.in.bootstrap;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class LoadtestScenarioTest {

    @Test
    void parsesConfiguredScenarioCaseInsensitively() {
        assertThat(LoadtestScenario.parse(" realistic ")).isEqualTo(LoadtestScenario.REALISTIC);
        assertThat(LoadtestScenario.parse("OVERLOAD")).isEqualTo(LoadtestScenario.OVERLOAD);
    }

    @Test
    void rejectsUnknownScenario() {
        assertThatThrownBy(() -> LoadtestScenario.parse("random"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("random");
    }
}
