package org.example.tonpad.core.files.directory;

import org.example.tonpad.TonpadConfig;
import org.example.tonpad.core.exceptions.IllegalInputException;
import org.example.tonpad.core.files.FileSystemService;
import org.example.tonpad.core.files.FileTree;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.nio.file.Files;
import java.nio.file.Path;

@Service
public class DirectoryServiceImpl implements DirectoryService {

    private final TonpadConfig.ReservedDirNames reservedNames;

    private final String dataDir;

    private final FileSystemService fileSystem;

    @Autowired
    public DirectoryServiceImpl(FileSystemService fileSystem, TonpadConfig config) {
        this.fileSystem = fileSystem;
        this.dataDir = config.dataPath();
        this.reservedNames = config.reservedNames();
    }

    public FileTree getFileTree(String path) {
        return fileSystem.getFileTree(path);
    }

    /**
     * Создает директорию со вложенными директориями для шаблонов и заметок
     * @param path путь до директории, в которой должна быть создана новая
     * @param name название новой директории
     * @return путь до новой директории
     */
    public Path createDir(Path path, String name) {
        if (reservedNames.templatesDir().equals(name) || reservedNames.notesDir().equals(name)) {
            throw new IllegalInputException("Нельзя создать директорию с таким названием");
        }

        return initDir(path.resolve(name));
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

    private Path initDir(Path path) {
        String stringPath = path.toString();

        fileSystem.makeDir(Path.of(stringPath, reservedNames.templatesDir()));
        fileSystem.makeDir(Path.of(stringPath, reservedNames.notesDir()));

        return path;
    }
}
