package org.example.tonpad.core.files;

import java.nio.file.Path;
import java.util.List;

public interface FileSystemService {

    FileTree getFileTree(String path);

    List<String> getAllFilesInDir(String directory);

    Path makeDir(String directory);

    Path makeFile(String path);

    List<String> readFile(String path);

    void write(String path, List<String> content);

    Path rename(String oldPath, String newPath);

    void delete(String path);
}
