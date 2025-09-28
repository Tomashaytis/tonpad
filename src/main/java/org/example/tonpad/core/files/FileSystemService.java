package org.example.tonpad.core.files;

import java.nio.file.Path;
import java.util.List;

public interface FileSystemService {

    FileTree getFileTree(String path);

    FileTree getFileTree(Path path);

    List<String> getAllFilesInDir(String directory);

    List<String> getAllFilesInDir(Path directory);

    Path makeDir(String directory);

    Path makeDir(Path directory);

    Path makeFile(String path);

    Path makeFile(Path path);

    List<String> readFile(String path);

    List<String> readFile(Path path);

    void write(String path, List<String> content);

    void write(Path path, List<String> content);

    Path rename(String oldPath, String newPath);

    Path rename(Path oldPath, Path newPath);

    void delete(String path);

    void delete(Path path);
}
