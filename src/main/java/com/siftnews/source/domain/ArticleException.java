package com.siftnews.source.domain;

import com.siftnews.common.BusinessException;

public class ArticleException extends BusinessException {

    public ArticleException(String message) {
        super(message);
    }
}
