package com.siftnews.delivery.domain;

import com.siftnews.common.BusinessException;

public class DeliveryException extends BusinessException {

    private final DeliveryFailureCategory category;

    public DeliveryException(DeliveryFailureCategory category, Throwable cause) {
        super(message(category, cause), cause);
        this.category = category;
    }

    public DeliveryFailureCategory getCategory() {
        return category;
    }

    public static String safeMessage(Throwable exception) {
        if (exception instanceof DeliveryException deliveryException) {
            return deliveryException.getMessage();
        }
        return message(DeliveryFailureCategory.UNKNOWN, exception);
    }

    private static String message(DeliveryFailureCategory category, Throwable cause) {
        String causeType = cause == null ? "Unknown" : cause.getClass().getSimpleName();
        return "메일 발송 실패: category=" + category + ", causeType=" + causeType;
    }
}
