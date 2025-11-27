package org.example.tonpad.core.files;

import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

import org.example.tonpad.core.sort.SortOptions;

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

    void writeFile(String path, String content);

    void writeFile(Path path, String content);

    Path rename(String oldPath, String newPath);

    Path rename(Path oldPath, Path newPath);

    void delete(String path);

    void delete(Path path);

    boolean exists(String path);

    boolean exists(Path path);

    FileTree getFileTreeSorted(String path, SortOptions opt);

    FileTree getFileTreeSorted(Path path, SortOptions opt);

    void copyFile(String path);

    void copyFile(Path path);

    void cutFile(String path);

    void cutFile(Path path);

    void pasteFile(String path);

    void pasteFile(Path path);

    boolean isMarkdownFile(String path);

    boolean isMarkdownFile(Path path);

    void showFileInNotepad(String path);

    void showFileInNotepad(Path path);

    void showFileInExplorer(String path);

    void showFileInExplorer(Path path);
}
