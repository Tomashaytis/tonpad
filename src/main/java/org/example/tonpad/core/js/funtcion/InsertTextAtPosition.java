package org.example.tonpad.core.js.funtcion;

import org.example.tonpad.TonpadConfig;
import org.example.tonpad.core.files.regularFiles.RegularFileService;
import org.springframework.stereotype.Component;

import java.util.List;

@Component("insertTextAtPosition")
public class InsertTextAtPosition extends JsFunction<Void> {

    public InsertTextAtPosition(TonpadConfig config, RegularFileService fileService) {
        super(config, fileService);
    }

    @Override
    public List<Class<?>> getParamsTypes() {
        return List.of(String.class, Integer.class);
    }

}
