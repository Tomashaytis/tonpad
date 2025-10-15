package org.example.tonpad.core.js.funtcion;

import lombok.Getter;
import org.example.tonpad.TonpadConfig;
import org.example.tonpad.core.files.regularFiles.RegularFileService;
import org.springframework.stereotype.Component;

import java.nio.file.Path;
import java.util.List;

@SuppressWarnings("unused")
public abstract class JsFunction<T> {

    private static final String FILE_EXTENSION = ".js";

    @Getter
    private final String jsCode;

    public JsFunction(TonpadConfig config, RegularFileService fileService) {
        Path directory = config.jsFunctionsDirectory();
        String fileName = getClass().getAnnotation(Component.class).value() + FILE_EXTENSION;

        jsCode = fileService.readFile(directory.resolve(fileName));
    }

    public abstract List<Class<?>> getParamsTypes();

}
