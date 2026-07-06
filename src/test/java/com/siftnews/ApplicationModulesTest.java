package com.siftnews;

import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModules;
import org.springframework.modulith.docs.Documenter;

/**
 * Modulith 모듈 경계 검증 — 각 모듈이 {@code allowedDependencies}로 선언한 의존만
 * 사용하는지 빌드 시점에 강제한다. 컨텍스트 로딩·DB 불필요.
 */
class ApplicationModulesTest {

    static final ApplicationModules MODULES = ApplicationModules.of(SiftApiApplication.class);

    @Test
    void verifiesModuleBoundaries() {
        MODULES.verify();
    }

    @Test
    void generatesModuleDocumentation() {
        new Documenter(MODULES).writeDocumentation();
    }
}
