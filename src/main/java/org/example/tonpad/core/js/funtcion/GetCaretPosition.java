package org.example.tonpad.core.js.funtcion;

import org.example.tonpad.TonpadConfig;
import org.example.tonpad.core.files.regularFiles.RegularFileService;
import org.springframework.stereotype.Component;

import java.util.List;

@Component("getCaretPosition")
public class GetCaretPosition extends JsFunction<Integer> {

    public GetCaretPosition(TonpadConfig config, RegularFileService fileService) {
        super(config, fileService);
    }

    @Override
    public List<Class<?>> getParamsTypes() {
        return List.of();
    }

}
