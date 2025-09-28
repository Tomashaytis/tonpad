package org.example.tonpad.core.files;

import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.tonpad.core.exceptions.CustomIOException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

@Service
@Slf4j
@NoArgsConstructor
public class FileSystemServiceImpl implements FileSystemService {

    private final static String DIR_READING_ERROR = "Ошибка при чтении директории";

    private final static String DIR_ALREADY_EXISTS_ERROR = "Директория с таким названием уже существует";

    private final static String DIR_CREATE_ERROR = "Ошибка при создании директории";

    private final static String FILE_READING_ERROR = "Ошибка при чтении файла";

    private final static String FILE_ALREADY_EXISTS_ERROR = "Файл с таким названием уже существует";

    private final static String FILE_CREATE_ERROR = "Ошибка при создании файла";

    private final static String FILE_READ_ERROR = "Ошибка при чтении файла";

    private final static String FILE_WRITE_ERROR = "Ошибка при записи в файл";

    private final static String RENAME_ERROR = "При переименовании произошла ошибка";

    private final static String DELETE_ERROR = "При удалении произошла ошибка";

    private final RecursiveDeleteFileVisitor visitor = new RecursiveDeleteFileVisitor();

    public FileTree getFileTree(String path) {
        return getFileTree(Path.of(path));
    }

    private FileTree getFileTree(Path path) {
        try (Stream<Path> elements = Files.list(path)) {
            List<FileTree> subtrees = new ArrayList<>();
            elements.forEach(el -> {
                if (Files.isDirectory(el)) {
                    subtrees.add(getFileTree(el));
                } else {
                    subtrees.add(new FileTree(Path.of(path.toString(), el.getFileName().toString()), null));
                }
            });
            return new FileTree(path, subtrees);
        } catch (IOException e) {
            log.error(DIR_READING_ERROR, e);
            throw new CustomIOException(DIR_READING_ERROR, e);
        }
    }

    public List<String> getAllFilesInDir(String directory) {
        Path path = Path.of(directory);

        try (Stream<Path> elements = Files.list(path)) {
            return elements.filter(Files::isRegularFile).map(i -> i.getFileName().toString()).toList();
        } catch (IOException e) {
            log.error(DIR_READING_ERROR, e);
            throw new CustomIOException(DIR_READING_ERROR, e);
        }
    }

    public Path makeDir(String directory) {
        Path path = Path.of(directory);

        try {
            return Files.createDirectories(path);
        } catch (FileAlreadyExistsException e) {
            log.error(DIR_ALREADY_EXISTS_ERROR, e);
            throw new CustomIOException(DIR_ALREADY_EXISTS_ERROR, e);
        } catch (IOException e) {
            log.error(DIR_CREATE_ERROR, e);
            throw new CustomIOException(DIR_CREATE_ERROR, e);
        }
    }

    public Path makeFile(String path) {
        Path filePath = Path.of(path);

        try {
            return Files.createFile(filePath);
        } catch (FileAlreadyExistsException e) {
            log.error(FILE_ALREADY_EXISTS_ERROR, e);
            throw new CustomIOException(FILE_ALREADY_EXISTS_ERROR, e);
        } catch (IOException e) {
            log.error(FILE_CREATE_ERROR, e);
            throw new CustomIOException(FILE_CREATE_ERROR, e);
        }
    }

    public List<String> readFile(String path) {
        Path filePath = Path.of(path);

        try {
            return Files.readAllLines(filePath);
        } catch (IOException e) {
            log.error(FILE_READ_ERROR, e);
            throw new CustomIOException(FILE_READ_ERROR, e);
        }
    }

    public void write(String path, List<String> content) {
        Path filePath = Path.of(path);

        try {
            Files.write(filePath, content);
        } catch (IOException e) {
            log.error(FILE_WRITE_ERROR, e);
            throw new CustomIOException(FILE_WRITE_ERROR, e);
        }
    }

    public Path rename(String oldPath, String newPath) {
        Path path = Path.of(oldPath);
        Path newDir = Path.of(newPath);

        if (!path.toFile().renameTo(newDir.toFile())) {
            throw new CustomIOException(RENAME_ERROR);
        }

        return newDir;
    }

    public void delete(String path) {
        Path deletePath = Path.of(path);

        try {
            Files.walkFileTree(deletePath, visitor);
        } catch (IOException e) {
            log.error(DELETE_ERROR, e);
            throw new CustomIOException(DELETE_ERROR, e);
        }
    }

    private static class RecursiveDeleteFileVisitor implements FileVisitor<Path> {
        @Override
        public @NotNull FileVisitResult preVisitDirectory(Path dir, @NotNull BasicFileAttributes attrs) throws IOException {
            return FileVisitResult.CONTINUE;
        }

        @Override
        public @NotNull FileVisitResult visitFile(Path file, @NotNull BasicFileAttributes attrs) throws IOException {
            Files.delete(file);
            return FileVisitResult.CONTINUE;
        }

        @Override
        public @NotNull FileVisitResult visitFileFailed(Path file, @NotNull IOException exc) throws IOException {
            log.error(FILE_READING_ERROR, exc);
            throw exc;
        }

        @Override
        public @NotNull FileVisitResult postVisitDirectory(Path dir, @Nullable IOException exc) throws IOException {
            Files.delete(dir);
            return FileVisitResult.CONTINUE;
        }
    }
}
