package org.example.tonpad.core.js.service.impl;

import lombok.RequiredArgsConstructor;
import org.example.tonpad.core.files.FileSystemService;
import org.example.tonpad.core.js.service.JsFilesService;
import org.springframework.stereotype.Service;

import java.nio.file.Path;

@Service
@RequiredArgsConstructor
public class JsFilesServiceImpl implements JsFilesService {

    private static final String FILE_EXTENSION = ".js";

    private static final String JS_DIRECTORY = System.getProperty("user.dir") + "\\src\\main\\resources\\js";

    private final FileSystemService fileSystemService;

    @Override
    public String readFile(String fileName) {
        return fileSystemService.readFile(Path.of(JS_DIRECTORY, fileName + FILE_EXTENSION));
    }

}
