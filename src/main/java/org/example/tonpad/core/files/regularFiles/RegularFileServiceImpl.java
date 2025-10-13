package org.example.tonpad.core.files.regularFiles;

import org.example.tonpad.core.exceptions.IllegalInputException;
import org.example.tonpad.core.files.FileSystemService;
import org.springframework.stereotype.Service;

import java.nio.file.Files;
import java.nio.file.Path;

@Service
public class RegularFileServiceImpl implements RegularFileService {

    private static final String FILE_EXTENSION = ".md";

    private final FileSystemService fileSystem;

    public RegularFileServiceImpl(FileSystemService fileSystem) {
        this.fileSystem = fileSystem;
    }

    public Path createFile(Path path, String name) {
        return fileSystem.makeFile(path.resolve(name + FILE_EXTENSION));
    }

    public String readFile(Path path) {
        return fileSystem.readFile(path);
    }

    public void writeFile(Path path, String content) {
        fileSystem.write(path, content);
    }

    public Path renameFile(Path path, String name) {
        if (!Files.isRegularFile(path)) {
            throw new IllegalInputException("Ошибка при переименовании файла");
        }

        return fileSystem.rename(path, Path.of(path.toAbsolutePath().getParent().toString(), name + FILE_EXTENSION));
    }

    public void deleteFile(Path path) {
        if (!Files.isRegularFile(path)) {
            throw new IllegalInputException("Ошибка при удалении файла");
        }

        fileSystem.delete(path);
    }
}
