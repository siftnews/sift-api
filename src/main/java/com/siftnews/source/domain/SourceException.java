package com.siftnews.source.domain;

import com.siftnews.common.BusinessException;

public class SourceException extends BusinessException {

    public SourceException(String message) {
        super(message);
    }
}
