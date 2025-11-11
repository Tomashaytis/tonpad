package org.example.tonpad.core.files.directory;

import org.example.tonpad.core.exceptions.IllegalInputException;
import org.example.tonpad.core.files.FileSystemService;
import org.example.tonpad.core.files.FileTree;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.nio.file.Files;
import java.nio.file.Path;

@Service
public class DirectoryServiceImpl implements DirectoryService {

    private final FileSystemService fileSystem;

    @Autowired
    public DirectoryServiceImpl(FileSystemService fileSystem) {
        this.fileSystem = fileSystem;
    }

    public boolean exists(Path path) {
        return fileSystem.exists(path);
    }

    public FileTree getFileTree(String path) {
        return fileSystem.getFileTree(path);
    }

    public Path createDir(Path path, String name) {
        return fileSystem.makeDir(path.resolve(name));
    }

    public Path renameDir(Path path, String name) {
        if (!Files.isDirectory(path)) {
            throw new IllegalInputException("Ошибка при переименовании директории");
        }

        return fileSystem.rename(path, Path.of(path.toAbsolutePath().getParent().toString(), name));
    }

    public void deleteDir(Path path) {
        if (!Files.isDirectory(path)) {
            throw new IllegalInputException("Ошибка при удалении директории");
        }

        fileSystem.delete(path);
    }
}
