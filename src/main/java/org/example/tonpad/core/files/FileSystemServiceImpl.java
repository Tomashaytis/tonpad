package org.example.tonpad.core.files;

import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.tonpad.core.exceptions.CustomIOException;
import org.example.tonpad.core.service.crypto.EncryptionService;
import org.example.tonpad.core.service.crypto.Impl.EncryptionServiceImpl;
import org.example.tonpad.core.service.crypto.exception.DecryptionException;
import org.example.tonpad.core.session.VaultSession;
import org.example.tonpad.core.sort.SortOptions;
import org.example.tonpad.ui.extentions.VaultPath;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javafx.scene.control.TreeItem;
import javafx.scene.input.Clipboard;
import org.springframework.util.FileSystemUtils;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

@Service
@Slf4j
@RequiredArgsConstructor
public class FileSystemServiceImpl implements FileSystemService {

    private final static String DIR_READING_ERROR = "Ошибка при чтении директории";

    private final static String DIR_ALREADY_EXISTS_ERROR = "Директория с таким названием уже существует";

    private final static String DIR_CREATE_ERROR = "Ошибка при создании директории";

    private final static String FILE_READING_ERROR = "Ошибка при чтении файла";

    private final static String FILE_ALREADY_EXISTS_ERROR = "Файл с таким названием уже существует";

    private final static String FILE_CREATE_ERROR = "Ошибка при создании файла";

    private final static String FILE_READ_ERROR = "Ошибка при чтении файла";

    private final static String FILE_WRITE_ERROR = "Ошибка при записи в файл";

    private final static String FILE_SEARCH_ERROR = "Ошибка при поиске файла";

    private final static String FILE_COPY_ERROR = "Ошибка при копировании файла";

    private final static String RENAME_ERROR = "При переименовании произошла ошибка";

    private final static String DELETE_ERROR = "При удалении произошла ошибка";

    private final static String FILE_OPENING_IN_EXPLORER_ERROR = "Ошибка при открытии файла в проводнике";

    private static final Logger log = LoggerFactory.getLogger(FileSystemServiceImpl.class);

    private final RecursiveDeleteFileVisitor visitor = new RecursiveDeleteFileVisitor();

    private final Buffer buffer;

    private final VaultPath vaultPath;

    private final VaultSession vaultSession;

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
        final String targetSubstring = substring.toLowerCase(java.util.Locale.ROOT);

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
        if (checkAccess(oldPath)) {
            if (!oldPath.toFile().renameTo(newPath.toFile())) {
            throw new CustomIOException(RENAME_ERROR);
            }
        }
        
        return newPath;
    }

    public void delete(String path) {
        delete(Path.of(path));
    }

    public void delete(Path path) {
        if (checkAccess(path)) {
            try {
                Files.walkFileTree(path, visitor);
            } catch (IOException e) {
                log.warn(DELETE_ERROR, e);
                throw new CustomIOException(DELETE_ERROR, e);
            }
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

    public void copyFile(Path path)
    {
        if (checkAccess(path)) {
            buffer.setCopyBuffer(List.of(path));
            buffer.setCutMode(false);
        }
    }
    public void copyFile(String path)
    {
        copyFile(Path.of(path));
    }

    public void cutFile(Path path)
    {
        if (checkAccess(path)) {
            buffer.setCopyBuffer(List.of(path));
            buffer.setCutMode(true);
        }
    }
    public void cutFile(String path)
    {
        cutFile(Path.of(path));
    }

    public void pasteFile(String targetDir)
    {
        pasteFile(Path.of(targetDir));
    }

    public void pasteFile(Path targetDir)
    {

        for(Path filePath: buffer.getCopyBuffer())
        {
            if (!buffer.isCutMode())
            {
                Path dst = uniqueDest(targetDir, filePath);
                try {
                    FileSystemUtils.copyRecursively(filePath, dst);
                } catch (IOException e) {
                    log.warn(FILE_COPY_ERROR);
                    throw new CustomIOException(FILE_COPY_ERROR, e);
                }
            }
            else
            {
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
    

    public void copyVaultPath()
    {
        var cc = new javafx.scene.input.ClipboardContent();
        cc.putString(vaultPath.getVaultPath());
        Clipboard.getSystemClipboard().setContent(cc);
    }

    public void copyAbsFilePath(String path)
    {
        var cc = new javafx.scene.input.ClipboardContent();
        cc.putString(path);
        Clipboard.getSystemClipboard().setContent(cc);
    }

    public void copyAbsFilePath(Path path)
    {
        copyAbsFilePath(path.toString());
    }
    
    public void copyRelFilePath(Path path)
    {
        var cc = new javafx.scene.input.ClipboardContent();
        cc.putString(path.toString());
        Clipboard.getSystemClipboard().setContent(cc);
    }

    public void copyRelFilePath(String absPath)
    {
        copyRelFilePath(Path.of(absPath));
    }

    public void showFileInExplorer(Path path)
    {
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
            default -> Comparator.comparing(p -> p.getFileName().toString());
        };

        if(opt.foldersFirst()) {
            Comparator<Path> byIsDirDesc = Comparator.<Path, Integer>comparing(p -> Files.isDirectory(p) ? 1 : 0).reversed();
            base = byIsDirDesc.thenComparing(base);
        }
        return base;
    }

    private boolean checkAccess(Path path)
    {
        if (vaultSession.isOpendWithNoPassword())
        {
            EncryptionService encoder = new EncryptionServiceImpl();
            if (encoder.isOpeningWithNoPasswordAllowed(path)) {
                return true;
            }
            return false;
        }
        else
        {
            try
            {
                String noteContent = Files.readString(path);

                byte[] key = vaultSession.getMasterKeyIfPresent()
                        .map(k -> k.getEncoded())
                        .orElse(null);
                EncryptionService encoder = new EncryptionServiceImpl(key);
                String resNoteContent = encoder.decrypt(noteContent, null);
                return true;
            }
            catch(IOException e)
            {
                log.info("reading note exeption");
                e.printStackTrace();
            }
            catch(DecryptionException e)
            {
                javafx.scene.control.Alert alert = new javafx.scene.control.Alert(
                        javafx.scene.control.Alert.AlertType.ERROR,
                        "Пошел нахер отсюда, это не для тебя сделано, и не для таких как ты. Не ходи, не засирай заметки, никому ты тут не нужен, тебя не звали сюда. Тебе тут не рады. Уйди отсюда и больше никогда не приходи.",
                        javafx.scene.control.ButtonType.OK
                );
                var owner = javafx.stage.Window.getWindows().stream()
                        .filter(javafx.stage.Window::isShowing)
                        .findFirst().orElse(null);
                if (owner != null) alert.initOwner(owner);
                alert.setTitle("Ошибка");
                alert.setHeaderText(null);

                ((javafx.scene.control.Label) alert.getDialogPane().lookup(".content.label")).setWrapText(true);
                alert.getDialogPane().setMinHeight(javafx.scene.layout.Region.USE_PREF_SIZE);

                alert.showAndWait();
                e.printStackTrace();
            }
            return false;
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
