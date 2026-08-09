package com.siftnews.subscriber.adapter.in.web;

import com.siftnews.subscriber.adapter.in.web.dto.ErrorResponse;
import com.siftnews.subscriber.domain.ConflictException;
import com.siftnews.subscriber.domain.SubscriberException;
import com.siftnews.subscriber.domain.SubscriberNotFoundException;
import com.siftnews.subscriber.domain.SubscriptionNotFoundException;
import com.siftnews.subscriber.domain.TopicNotFoundException;
import jakarta.validation.ConstraintViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class SubscriberExceptionHandler {

    @ExceptionHandler({
            SubscriberNotFoundException.class,
            SubscriptionNotFoundException.class,
            TopicNotFoundException.class
    })
    ResponseEntity<ErrorResponse> notFound(SubscriberException exception) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ErrorResponse(exception.getMessage()));
    }

    @ExceptionHandler(ConflictException.class)
    ResponseEntity<ErrorResponse> conflict(ConflictException exception) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(new ErrorResponse(exception.getMessage()));
    }

    @ExceptionHandler({
            SubscriberException.class,
            MethodArgumentNotValidException.class,
            ConstraintViolationException.class
    })
    ResponseEntity<ErrorResponse> badRequest(Exception exception) {
        return ResponseEntity.badRequest().body(new ErrorResponse(exception.getMessage()));
    }
}
