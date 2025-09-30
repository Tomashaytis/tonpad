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
import java.util.Optional;
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

    private final static String FILE_SEARCH_ERROR = "Ошибка при записи в файл";

    private final static String RENAME_ERROR = "При переименовании произошла ошибка";

    private final static String DELETE_ERROR = "При удалении произошла ошибка";

    private final RecursiveDeleteFileVisitor visitor = new RecursiveDeleteFileVisitor();

    public FileTree getFileTree(String path) {
        return getFileTree(Path.of(path));
    }

    public FileTree getFileTree(Path path) {
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
            log.warn(DIR_READING_ERROR, e);
            throw new CustomIOException(DIR_READING_ERROR, e);
        }
    }

    public Optional<Path> findFileInDir(Path rootDir, String fileName) {
        try (Stream<Path> elems = Files.walk(rootDir)) {
            return elems.filter(el -> el.getFileName().toString().equals(fileName)).findFirst();
        } catch (IOException e) {
            log.warn(FILE_SEARCH_ERROR, e);
            throw new CustomIOException(FILE_SEARCH_ERROR, e);
        }
    }

    public List<String> getAllFilesInDir(String directory) {
        return getAllFilesInDir(Path.of(directory));
    }

    public List<String> getAllFilesInDir(Path directory) {
        try (Stream<Path> elements = Files.list(directory)) {
            return elements.filter(Files::isRegularFile).map(i -> i.getFileName().toString()).toList();
        } catch (IOException e) {
            log.warn(DIR_READING_ERROR, e);
            throw new CustomIOException(DIR_READING_ERROR, e);
        }
    }

    public Path makeDir(String directory) {
        return makeDir(Path.of(directory));
    }

    public Path makeDir(Path directory) {
        if (Files.exists(directory)) {
            log.warn(DIR_ALREADY_EXISTS_ERROR);
            throw new CustomIOException(DIR_ALREADY_EXISTS_ERROR);
        }

        try {
            return Files.createDirectories(directory);
        } catch (IOException e) {
            log.warn(DIR_CREATE_ERROR, e);
            throw new CustomIOException(DIR_CREATE_ERROR, e);
        }
    }

    public Path makeFile(String path) {
        return makeFile(Path.of(path));
    }

    public Path makeFile(Path path) {
        try {
            return Files.createFile(path);
        } catch (FileAlreadyExistsException e) {
            log.warn(FILE_ALREADY_EXISTS_ERROR, e);
            throw new CustomIOException(FILE_ALREADY_EXISTS_ERROR, e);
        } catch (IOException e) {
            log.warn(FILE_CREATE_ERROR, e);
            throw new CustomIOException(FILE_CREATE_ERROR, e);
        }
    }

    public String readFile(String path) {
        return readFile(Path.of(path));
    }

    public String readFile(Path path) {
        try {
            return Files.readString(path);
        } catch (IOException e) {
            log.warn(FILE_READ_ERROR, e);
            throw new CustomIOException(FILE_READ_ERROR, e);
        }
    }

    public void write(String path, String content) {
        write(Path.of(path), content);
    }

    public void write(Path path, String content) {
        try {
            Files.writeString(path, content);
        } catch (IOException e) {
            log.warn(FILE_WRITE_ERROR, e);
            throw new CustomIOException(FILE_WRITE_ERROR, e);
        }
    }

    public Path rename(String oldPath, String newPath) {
        return rename(Path.of(oldPath), Path.of(newPath));
    }

    public Path rename(Path oldPath, Path newPath) {
        if (!oldPath.toFile().renameTo(newPath.toFile())) {
            throw new CustomIOException(RENAME_ERROR);
        }

        return newPath;
    }

    public void delete(String path) {
        delete(Path.of(path));
    }

    public void delete(Path path) {
        try {
            Files.walkFileTree(path, visitor);
        } catch (IOException e) {
            log.warn(DELETE_ERROR, e);
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
