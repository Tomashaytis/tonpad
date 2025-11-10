package org.example.tonpad.core.exceptions.handler;

import java.lang.reflect.Method;

public class HandlerMethod {

    final Class<? extends Exception> exceptionType;

    final Method method;

    HandlerMethod(Class<? extends Exception> exceptionType, Method method) {
        this.exceptionType = exceptionType;
        this.method = method;
    }

}
