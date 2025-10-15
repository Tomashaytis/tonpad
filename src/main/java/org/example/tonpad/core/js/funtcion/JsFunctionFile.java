package org.example.tonpad.core.js.funtcion;

import lombok.Getter;
import org.example.tonpad.core.exceptions.IllegalJsArgumentException;

import java.util.List;

@SuppressWarnings("unused")
public abstract class JsFunctionFile<T> {

    @Getter
    private final List<Object> params;

    protected JsFunctionFile(Object... params) {
        List<Class<?>> argumentsTypes = getParamsTypes();

        if (params.length != argumentsTypes.size()) {
            throw new IllegalJsArgumentException("неверное число аргументов");
        }

        for (int i = 0; i < params.length; ++i) {
            Class<?> provided = params[i].getClass();
            Class<?> expected = argumentsTypes.get(i);
            if (provided != expected) {
                throw new IllegalJsArgumentException("неверный тип аргумента " + provided + ", ожидается " + expected);
            }
        }

        this.params = List.of(params);
    }

    public abstract String getFileName();

    protected abstract List<Class<?>> getParamsTypes();

}
