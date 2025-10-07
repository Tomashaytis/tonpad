package org.example.tonpad.core.files.regularFiles;

import java.nio.file.Path;

public interface RegularFileService {

    Path createFile(Path path, String name);

    String readFile(Path path);

    void writeFile(Path path, String content);

    Path renameFile(Path path, String name);

    void deleteFile(Path path);
}
