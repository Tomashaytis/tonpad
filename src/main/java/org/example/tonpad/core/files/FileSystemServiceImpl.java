package org.example.tonpad.core.files;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.tonpad.core.exceptions.CustomIOException;
import org.example.tonpad.core.exceptions.TonpadBaseException;
import org.example.tonpad.core.service.crypto.Encryptor;
import org.example.tonpad.core.service.crypto.EncryptorFactory;
import org.example.tonpad.core.service.crypto.Impl.AesGcmEncryptor;
import org.example.tonpad.core.exceptions.DecryptionException;
import org.example.tonpad.core.session.VaultSession;
import org.example.tonpad.core.sort.SortOptions;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.stereotype.Service;

import org.springframework.util.FileSystemUtils;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.security.Key;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

@Service
@Slf4j
@RequiredArgsConstructor
public class FileSystemServiceImpl implements FileSystemService {

    private final static String DIR_READING_ERROR = "Reading directory error";

    private final static String DIR_ALREADY_EXISTS_ERROR = "Directory already exists";

    private final static String DIR_CREATE_ERROR = "Directory creation error";

    private final static String FILE_READING_ERROR = "File reading error";

    private final static String FILE_ALREADY_EXISTS_ERROR = "File already exists";

    private final static String FILE_CREATE_ERROR = "File creation error";

    private final static String FILE_READ_ERROR = "File reading error";

    private final static String FILE_WRITE_ERROR = "File write error";

    private final static String FILE_SEARCH_ERROR = "File search error";

    private final static String FILE_COPY_ERROR = "File copy error";

    private final static String RENAME_ERROR = "Rename error";

    private final static String DELETE_ERROR = "Delete error";

    private final static String FILE_OPENING_IN_EXPLORER_ERROR = "Explorer opening error";

    private final RecursiveDeleteFileVisitor visitor = new RecursiveDeleteFileVisitor();

    private final Buffer buffer;

    private final VaultSession vaultSession;

    private final EncryptorFactory encryptorFactory;

    public FileTree getFileTree(String path) {
        return getFileTree(Path.of(path));
    }

    public FileTree getFileTree(Path path) {
        SortOptions opt = SortOptions.defaults();
        return getFileTreeSorted(path, opt);
    }

    public Optional<Path> findFileInDir(Path rootDir, String fileName) {
        try (Stream<Path> elems = Files.walk(rootDir)) {
            return elems.filter(el -> el.getFileName().toString().equals(fileName)).findFirst();
        } catch (IOException e) {
            log.warn(FILE_SEARCH_ERROR, e);
            throw new CustomIOException(FILE_SEARCH_ERROR, e);
        }
    }

    public List<Path> findByNameContains(String rootDir, String substring)
    {
        return findByNameContains(Path.of(rootDir), substring);
    }

    public List<Path> findByNameContains(Path rootDir, String substring) {
        if (substring == null || substring.isBlank()) return List.of();

        try (Stream<Path> elems = Files.walk(rootDir))
        {
            return elems.filter(p -> !p.equals(rootDir)).map(rootDir::relativize).filter(path -> path.getFileName().toString().contains(substring)).toList();
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

    public void writeFile(String path, String content) {
        writeFile(Path.of(path), content);
    }

    public void writeFile(Path path, String content) {
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

    public boolean exists(String path) {
        return exists(Path.of(path));
    }

    public boolean exists(Path path) {
        return  Files.exists(path);
    }

    public FileTree getFileTreeSorted(String path, SortOptions opt) {
        return getFileTreeSorted(Path.of(path), opt);
    }

    public FileTree getFileTreeSorted(Path path, SortOptions opt) {
        try (Stream<Path> stream = Files.list(path)) {
            List<Path> children = stream.filter(p -> !p.equals(path)).filter(p -> {
                if(!opt.relevantOnly()) return true;
                if(Files.isDirectory(p)) return true;
                String name = p.getFileName() == null ? "" : p.getFileName().toString().toLowerCase();
                return name.endsWith(".md");
            }).toList();

            Comparator<Path> cmp = makeComparator(opt);

            List<FileTree> subtrees = new ArrayList<>(children.size());
            children.stream().sorted(cmp).forEach(p -> {
                if(Files.isDirectory(p)) subtrees.add(getFileTreeSorted(p, opt));
                else subtrees.add(new FileTree(path.resolve(p.getFileName()), null));
            });

            return new FileTree(path, subtrees);
        }
        catch (IOException e) {
            log.warn(DIR_READING_ERROR);
            throw new CustomIOException(DIR_READING_ERROR, e);
        }
    }

    public void copyFile(Path path) {
        buffer.setCopyBuffer(List.of(path));
        buffer.setCutMode(false);
    }
    public void copyFile(String path)
    {
        copyFile(Path.of(path));
    }

    public void cutFile(Path path) {
        buffer.setCopyBuffer(List.of(path));
        buffer.setCutMode(true);
    }
    public void cutFile(String path)
    {
        cutFile(Path.of(path));
    }

    public void pasteFile(String targetDir)
    {
        pasteFile(Path.of(targetDir));
    }

    public void pasteFile(Path targetDir) {

        for(Path filePath: buffer.getCopyBuffer()) {
            if (!buffer.isCutMode()) {
                Path dst = uniqueDest(targetDir, filePath);
                try {
                    FileSystemUtils.copyRecursively(filePath, dst);
                } catch (IOException e) {
                    log.warn(FILE_COPY_ERROR);
                    throw new CustomIOException(FILE_COPY_ERROR, e);
                }
            } else {
                try {
                    Path dst = targetDir.resolve(filePath.getFileName());

                    if (!Files.exists(dst))
                        Files.move(filePath, dst);
                } catch (IOException e) {
                    log.warn(FILE_COPY_ERROR);
                    throw new CustomIOException(FILE_COPY_ERROR, e);
                }
            }
        }
    }

    public void showFileInExplorer(Path path) {
        var file = path.toFile();
        var os = System.getProperty("os.name").toLowerCase();

        try {
            if (os.contains("win")) {
                if (file.isFile()) {
                    new ProcessBuilder("explorer.exe", "/select,", file.getAbsolutePath()).start();
                } else {
                    new ProcessBuilder("explorer.exe", file.getAbsolutePath()).start();
                }
            } else if (os.contains("mac")) {
                if (file.isFile()) {
                    new ProcessBuilder("open", "-R", file.getAbsolutePath()).start();
                } else {
                    new ProcessBuilder("open", file.getAbsolutePath()).start();
                }
            } else {
                File dir = file.isDirectory() ? file : file.getParentFile();
                if (dir != null) {
                    try {
                        new ProcessBuilder("xdg-open", dir.getAbsolutePath()).start();
                    } catch (Exception ignore) {
                        java.awt.Desktop.getDesktop().open(dir);
                    }
                }
            }
        } catch (Exception ex) {
            log.warn(FILE_OPENING_IN_EXPLORER_ERROR);
            throw new CustomIOException(FILE_OPENING_IN_EXPLORER_ERROR, ex);
        }
    }

    public void showFileInExplorer(String path)
    {
        showFileInExplorer(Path.of(path));
    }

    private static Path uniqueDest(Path targetDir, Path src) {
        String name = src.getFileName().toString();

        String base;
        String ext = "";
        if (!Files.isDirectory(src)) {
            int dot = name.lastIndexOf('.');
            if (dot > 0 && dot < name.length() - 1) {
                base = name.substring(0, dot);
                ext  = name.substring(dot);
            } else {
                base = name;
            }
        } else {
            base = name;
        }

        Path candidate = targetDir.resolve(name);
        if (!Files.exists(candidate)) return candidate;

        int n = 1;
        while (true) {
            String suffix = (n == 1) ? " copy" : " copy " + n;
            String newName = base + suffix + ext;
            candidate = targetDir.resolve(newName);
            if (!Files.exists(candidate)) return candidate;
            n++;
        }
    }

    private Comparator<Path> makeComparator(SortOptions opt) {
        Comparator<Path> byName = Comparator.comparing(
            (Path p) -> {
                Path fn = p.getFileName();
                return (fn == null ? p.toString() : fn.toString());
            },
            String.CASE_INSENSITIVE_ORDER
        );

        var byCreated = java.util.Comparator.comparingLong((Path p) -> {
            try {
                BasicFileAttributes attr = Files.readAttributes(p, BasicFileAttributes.class, LinkOption.NOFOLLOW_LINKS);
                FileTime time = attr.creationTime();
                if (time != null) return time.toMillis();
                return attr.lastModifiedTime().toMillis();
            } catch (IOException ex) {
                return 0L;
            }
        });

        Comparator<Path> base = switch (opt.key()) {
            case NAME_ASC -> byName;
            case NAME_DESC -> byName.reversed();
            case CREATED_NEWEST -> byCreated.reversed().thenComparing(byName);
            case CREATED_OLDEST -> byCreated.thenComparing(byName);
        };

        if(opt.foldersFirst()) {
            Comparator<Path> byIsDirDesc = Comparator.<Path, Integer>comparing(p -> Files.isDirectory(p) ? 1 : 0).reversed();
            base = byIsDirDesc.thenComparing(base);
        }
        return base;
    }

    private static class RecursiveDeleteFileVisitor implements FileVisitor<Path> {
        @Override
        public @NotNull FileVisitResult preVisitDirectory(Path dir, @NotNull BasicFileAttributes attrs) {
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
