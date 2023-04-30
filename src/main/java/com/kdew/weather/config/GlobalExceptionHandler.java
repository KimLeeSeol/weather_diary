package com.kdew.weather.config;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {
    // 전역 예외 설정
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR) // 예외가 발생한 시점이 클라이언트에서 서버 api를 호출한 시점도 있기 때문
    @ExceptionHandler(Exception.class)
    public Exception handleAllException() {
        System.out.println("error from GlobalExceptionHandler");
        return new Exception();
    }
}
