package com.siftnews.delivery.adapter.out.email;

import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Profile;

import static org.assertj.core.api.Assertions.assertThat;

class EmailAdapterProfileTest {

    @Test
    void selectsOnlyOneAdapterForLoadtestProfile() {
        assertThat(LocalSmtpAdapter.class.getAnnotation(Profile.class).value())
                .containsExactly("!test & !loadtest");
        assertThat(LoadtestSendEmailAdapter.class.getAnnotation(Profile.class).value())
                .containsExactly("loadtest & !test");
    }
}
