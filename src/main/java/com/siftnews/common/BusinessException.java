package com.siftnews.common;

/**
 * 비즈니스 규칙 위반을 나타내는 공통 예외의 기반 타입.
 * <p>
 * 각 모듈은 이 타입을 상속하거나 그대로 던져 도메인 오류를 표현한다.
 * (에러 코드 체계는 필요해지는 시점에 확장한다 — 과설계 회피.)
 */
public class BusinessException extends RuntimeException {

    public BusinessException(String message) {
        super(message);
    }

    public BusinessException(String message, Throwable cause) {
        super(message, cause);
    }
}
