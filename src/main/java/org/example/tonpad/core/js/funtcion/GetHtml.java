package org.example.tonpad.core.js.funtcion;

import java.util.List;

import org.example.tonpad.TonpadConfig;
import org.example.tonpad.core.files.regularFiles.RegularFileService;
import org.springframework.stereotype.Component;

@Component("getHtml")
public class GetHtml extends JsFunction<String> {
    public GetHtml(TonpadConfig config, RegularFileService fileService) { super(config, fileService); }
    
    @Override 
    public List<Class<?>> getParamsTypes() { return List.of(); }
}
