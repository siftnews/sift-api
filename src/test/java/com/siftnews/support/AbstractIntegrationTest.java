package com.siftnews.support;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.containers.PostgreSQLContainer;

import static com.siftnews.support.TestContainerImages.POSTGRES_IMAGE;

/**
 * 통합 테스트 공통 베이스 — Testcontainers PostgreSQL 위에서 {@code test} 프로파일로 구동.
 * <p>
 * {@code @ServiceConnection}이 컨테이너의 datasource 를 스프링에 자동 연결한다.
 * <b>테스트는 절대 local 프로파일 금지</b>(CLAUDE.md) → 이 클래스를 상속해 사용한다.
 * <p>
 * 컨테이너는 static 블록에서 한 번만 기동하는 싱글톤 패턴(Testcontainers 공식 권장)이다.
 * {@code @Container} 라이프사이클을 쓰면 서브클래스(테스트)마다 stop→restart 되어 포트가
 * 바뀌고, Spring의 ApplicationContext 캐시가 옛 포트를 참조해 연결 실패로 이어진다.
 * JVM 종료 시 Ryuk이 정리한다.
 * <p>
 * 이 클래스 자체는 abstract라 테스트 대상으로 실행되지 않지만, 이를 상속한 테스트가
 * 실행되면 static 블록에서 Docker 컨테이너가 기동되므로 Docker가 필요하다.
 */
@SpringBootTest
@ActiveProfiles("test")
@Import(FixedClockTestConfig.class)
public abstract class AbstractIntegrationTest {

    @ServiceConnection
    static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>(POSTGRES_IMAGE);

    static {
        POSTGRES.start();
    }
}
