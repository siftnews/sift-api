package com.siftnews.content.domain;

import com.siftnews.common.BusinessException;

public class ContentException extends BusinessException {

    public ContentException(String message) {
        super(message);
    }

    public ContentException(String message, Throwable cause) {
        super(message, cause);
    }
}
