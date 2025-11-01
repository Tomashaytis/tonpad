package org.example.tonpad.core.js.funtcion;

import java.util.List;

import org.example.tonpad.TonpadConfig;
import org.example.tonpad.core.files.regularFiles.RegularFileService;
import org.springframework.stereotype.Component;

@Component("clearMarks")
public class ClearMarks extends JsFunction<Integer> {
    public ClearMarks(TonpadConfig cfg, RegularFileService fs) { super(cfg, fs); }

    @Override 
    public List<Class<?>> getParamsTypes() { return List.of(); }
}
