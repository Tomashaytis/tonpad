package org.example.tonpad.core.exceptions.handler;

import lombok.Getter;

import java.lang.reflect.Method;

@Getter
public class HandlerMethod {

    private final Class<? extends Exception> exceptionType;

    private final Method method;

    HandlerMethod(Class<? extends Exception> exceptionType, Method method) {
        this.exceptionType = exceptionType;
        this.method = method;
    }

}
