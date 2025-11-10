package org.example.tonpad.core.exceptions.handler;

import java.lang.reflect.Method;

public record HandlerMethod(Class<? extends Exception> exceptionType, Method method) {
}
