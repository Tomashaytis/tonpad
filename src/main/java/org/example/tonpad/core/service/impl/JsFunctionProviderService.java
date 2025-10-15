package org.example.tonpad.core.service.impl;

import jakarta.annotation.PostConstruct;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.example.tonpad.core.files.FileSystemService;
import org.springframework.stereotype.Service;

import java.nio.file.Path;

@Service
@RequiredArgsConstructor
@Getter
public class JsFunctionProviderService {

    private static final String FILE_EXTENSION = ".js";

    @Getter(value = AccessLevel.PRIVATE)
    private final FileSystemService fileSystemService;

    @Getter(value = AccessLevel.PRIVATE)
    private final String jsDirectory = System.getProperty("user.dir") + "\\src\\main\\resources\\js";

    private String getCaretPosition;

    private String insertTextAtPosition;

    @PostConstruct
    private void postConstruct() {
        getCaretPosition = readFile("getCaretPosition");
        insertTextAtPosition = readFile("insertTextAtPosition");
    }

    private String readFile(String fileName) {
        return fileSystemService.readFile(Path.of(jsDirectory, fileName + FILE_EXTENSION));
    }

}
