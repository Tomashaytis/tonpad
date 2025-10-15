package org.example.tonpad.core.js.funtcion;

import java.util.List;

public class InsertTextAtPosition extends JsFunctionFile<Void> {

    public InsertTextAtPosition(String text, Integer position) {
        super(text, position);
    }

    @Override
    public String getFileName() {
        return "insertTextAtPosition";
    }

    @Override
    protected List<Class<?>> getParamsTypes() {
        return List.of(String.class, Integer.class);
    }

}
