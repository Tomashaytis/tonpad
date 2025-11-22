package org.example.tonpad.core.files;

import java.io.IOException;
import java.nio.file.*;
import java.util.Set;
import java.util.stream.Stream;

import org.example.tonpad.core.exceptions.CustomIOException;
import org.example.tonpad.core.service.crypto.Encryptor;
import org.example.tonpad.core.service.crypto.EncryptorFactory;
import org.example.tonpad.core.service.crypto.Impl.AesGcmEncryptor;
import org.example.tonpad.core.exceptions.DecryptionException;
import org.example.tonpad.core.exceptions.EncryptionException;
import org.example.tonpad.core.session.VaultSession;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@RequiredArgsConstructor
public class CryptoFileServiceImpl implements CryptoFileService {
    private final FileSystemService fileSystemService;
    private final VaultSession vaultSession;
    private final EncryptorFactory encryptorFactory;
    private final EncryptorFactory encryptorFactory;

    // Разрешенные расширения для обработки (allow-list)
    private static final Set<String> ALLOWED_EXT = Set.of(
        "md", "markdown", "mdx"
    );

    private static final Set<String> NAME_SKIP = Set.of(
        ".DS_Store", "Thumbs.db", "desktop.ini"
    );

    private static final Set<String> DIR_SKIP = Set.of(
        ".git", ".idea", "node_modules"
    );

    private static boolean looksBackupOrTemp(String name) {
        return name.endsWith("~") || name.startsWith("~$") || name.startsWith(".$");
    }

    // Разрешен ли файл к обработке по пути и расширению
    private boolean shouldProcess(Path file) {
        try {
            if (!Files.isRegularFile(file) || Files.isSymbolicLink(file)) return false;

            // пропускаем скрытые/служебные
            String base = file.getFileName().toString();
            if (base.startsWith(".")) return false;
            try {
                if (Files.isHidden(file)) return false;
            } catch (IOException ignore) { }
            if (NAME_SKIP.contains(base)) return false;
            if (looksBackupOrTemp(base)) return false;

            // только разрешенные расширения
            int dot = base.lastIndexOf('.');
            if (dot <= 0 || dot >= base.length() - 1) return false;
            String ext = base.substring(dot + 1).toLowerCase();
            if (!ALLOWED_EXT.contains(ext)) return false;

            // исключаем каталоги по имени в пути
            for (Path p : file) {
                String seg = p.toString();
                if (DIR_SKIP.contains(seg)) return false;
            }
            return true;
        } catch (Exception e) {
            log.debug("skip {} due to filter error: {}", file, e.toString());
            return false;
        }
    }

    // Наш формат шифрования определяется заголовком
    private static boolean looksEncrypted(String data) {
        return data != null && data.startsWith(AesGcmEncryptor.HEADER);
        return data != null && data.startsWith(AesGcmEncryptor.HEADER);
    }

    @Override
    public void reEncryptFiles(byte[] oldKeyOrNull, byte[] newKey, Path root) {
        final boolean noPwdMode = vaultSession.isOpendWithNoPassword();
        final boolean withKeyMode = vaultSession.isProtectionEnabled();

        log.info("[REENCRYPT] start: root='{}', withKey={}, noPwd={}, oldKeyPresent={}, newKeyLen={}",
                root, withKeyMode, noPwdMode, (oldKeyOrNull != null), (newKey == null ? 0 : newKey.length));

        Encryptor decryptor = (oldKeyOrNull != null) ? encryptorFactory.encryptorForKey(oldKeyOrNull) : null;
        Encryptor encryptor = encryptorFactory.encryptorForKey(newKey);
        Encryptor decryptor = (oldKeyOrNull != null) ? encryptorFactory.encryptorForKey(oldKeyOrNull) : null;
        Encryptor encryptor = encryptorFactory.encryptorForKey(newKey);

        long total = 0, filteredOut = 0, skipped = 0, changed = 0, errors = 0;

        try (Stream<Path> files = Files.walk(root)) {
            for (Path file : (Iterable<Path>) files::iterator) {
                total++;
                if (!shouldProcess(file)) { filteredOut++; continue; }

                try {
                    String data = fileSystemService.readFile(file);
                    boolean isEnc = looksEncrypted(data);

                    if (noPwdMode) {
                        // Шифруем только открытые .md, поверх
                        if (isEnc) { skipped++; continue; }
                        String encrypted = encryptor.encrypt(data == null ? "" : data, null);
                        fileSystemService.writeFile(file, encrypted);
                        changed++;
                    } else {
                        // Перешифрование только наших .md, уже зашифрованных
                        if (!isEnc) { skipped++; continue; }
                        if (decryptor == null) { skipped++; continue; }
                        String plain = decryptor.decrypt(data, null);
                        String reenc = encryptor.encrypt(plain == null ? "" : plain, null);
                        fileSystemService.writeFile(file, reenc);
                        changed++;
                    }
                } catch (DecryptionException e) {
                    // «Чужие» зашифрованные — пропускаем
                    skipped++;
                } catch (EncryptionException e) {
                    errors++;
                    log.info("[REENCRYPT] encrypt error {}: {}", file, e.toString());
                } catch (Exception e) {
                    errors++;
                    log.info("[REENCRYPT] io error {}: {}", file, e.toString());
                }
            }
        } catch (IOException e) {
            throw new CustomIOException(e.getMessage());
        }

        log.info("[REENCRYPT] done: total={}, filteredOut={}, changed={}, skipped={}, errors={}",
                total, filteredOut, changed, skipped, errors);
    }

    @Override
    public void decryptFiles(byte[] oldKey, Path root) {
        log.info("[DECRYPT] start: root='{}', keyLen={}", root, oldKey == null ? 0 : oldKey.length);
        if (oldKey == null) {
            log.info("[DECRYPT] no key -> nothing to decrypt");
            return;
        }
        Encryptor decryptor = encryptorFactory.encryptorForKey(oldKey);
        Encryptor decryptor = encryptorFactory.encryptorForKey(oldKey);

        long total = 0, filteredOut = 0, changed = 0, skipped = 0, errors = 0;
        try (Stream<Path> files = Files.walk(root)) {
            for (Path file : (Iterable<Path>) files::iterator) {
                total++;
                if (!shouldProcess(file)) { filteredOut++; continue; }

                try {
                    String data = fileSystemService.readFile(file);
                    if (!looksEncrypted(data)) { skipped++; continue; }

                    String decrypted = decryptor.decrypt(data, null);
                    fileSystemService.writeFile(file, decrypted);
                    changed++;
                } catch (DecryptionException e) {
                    skipped++;
                } catch (Exception e) {
                    errors++;
                    log.info("[DECRYPT] error {}: {}", file, e.toString());
                }
            }
        } catch (IOException e) {
            throw new CustomIOException(e.getMessage());
        }
        log.info("[DECRYPT] done: total={}, filteredOut={}, changed={}, skipped={}, errors={}",
                total, filteredOut, changed, skipped, errors);
    }

    @Override
    public void encryptFiles(byte[] newKey, Path root) {
        log.info("[ENCRYPT] start: root='{}', keyLen={}", root, newKey == null ? 0 : newKey.length);
        Encryptor encryptor = encryptorFactory.encryptorForKey(newKey);
        Encryptor encryptor = encryptorFactory.encryptorForKey(newKey);

        long total = 0, filteredOut = 0, changed = 0, skipped = 0, errors = 0;
        try (Stream<Path> files = Files.walk(root)) {
            for (Path file : (Iterable<Path>) files::iterator) {
                total++;
                if (!shouldProcess(file)) { filteredOut++; continue; }

                try {
                    String data = fileSystemService.readFile(file);
                    if (looksEncrypted(data)) { skipped++; continue; }

                    String encrypted = encryptor.encrypt(data == null ? "" : data, null);
                    fileSystemService.writeFile(file, encrypted);
                    changed++;
                } catch (EncryptionException e) {
                    errors++;
                    log.info("[ENCRYPT] encrypt error {}: {}", file, e.toString());
                } catch (Exception e) {
                    errors++;
                    log.info("[ENCRYPT] error {}: {}", file, e.toString());
                }
            }
        } catch (IOException e) {
            throw new CustomIOException(e.getMessage());
        }
        log.info("[ENCRYPT] done: total={}, filteredOut={}, changed={}, skipped={}, errors={}",
                total, filteredOut, changed, skipped, errors);
    }
}
