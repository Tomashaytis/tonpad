package org.example.tonpad.core.files.directory;

import org.example.tonpad.core.files.FileTree;

import java.nio.file.Path;

public interface DirectoryService {

    FileTree getFileTree(String path);

    Path createDir(Path path, String name);

    Path renameDir(Path path, String name);

    void deleteDir(Path path);
}
