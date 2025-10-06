package org.example.tonpad.core.files;

import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

public interface FileSystemService {

    FileTree getFileTree(String path);

    FileTree getFileTree(Path path);

    Optional<Path> findFileInDir(Path rootDir, String fileName);

    List<Path> findByNameContains(Path rootDir, String substring);

    List<Path> findByNameContains(String rootDir, String substring);

    List<String> getAllFilesInDir(String directory);

    List<String> getAllFilesInDir(Path directory);

    Path makeDir(String directory);

    Path makeDir(Path directory);

    Path makeFile(String path);

    Path makeFile(Path path);

    String readFile(String path);

    String readFile(Path path);

    void write(String path, String content);

    void write(Path path, String content);

    Path rename(String oldPath, String newPath);

    Path rename(Path oldPath, Path newPath);

    void delete(String path);

    void delete(Path path);

    boolean exists(String path);

    boolean exists(Path path);
}
