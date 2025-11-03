package org.example.tonpad.core.js.funtcion;

import java.util.List;

import org.example.tonpad.TonpadConfig;
import org.example.tonpad.core.files.regularFiles.RegularFileService;
import org.springframework.stereotype.Component;

@Component("clearDomSelection")
public class ClearDomSelection extends JsFunction<Boolean> {
    public ClearDomSelection(TonpadConfig config, RegularFileService fileService) { super(config, fileService); }

    @Override
    public List<Class<?>> getParamsTypes() { return List.of(); }
}
