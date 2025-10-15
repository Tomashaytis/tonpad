package org.example.tonpad.core.js.funtcion;

import java.util.List;

public class GetCaretPosition extends JsFunctionFile<Integer> {

    @Override
    public String getFileName() {
        return "getCaretPosition";
    }

    @Override
    protected List<Class<?>> getParamsTypes() {
        return List.of();
    }

}
