package org.example.tonpad.core.files;

import java.io.IOException;
import java.nio.file.*;
import java.util.Set;
import java.util.stream.Stream;

import org.example.tonpad.core.exceptions.CustomIOException;
import org.example.tonpad.core.service.crypto.EncryptionService;
import org.example.tonpad.core.service.crypto.Impl.EncryptionServiceImpl;
import org.example.tonpad.core.service.crypto.exception.DecryptionException;
import org.example.tonpad.core.service.crypto.exception.EncryptionException;
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

    private static final Set<String> EXT_SKIP = Set.of(
        "db","sqlite","sqlite3","ldb","log","tmp","bak","swp","swx"
    );
    private static final Set<String> NAME_SKIP = Set.of(
        ".DS_Store","Thumbs.db","desktop.ini"
    );
    private static final Set<String> DIR_SKIP = Set.of(
        ".git",".idea","node_modules"
    );

    private static boolean looksBackupOrTemp(String name) {
        return name.endsWith("~") || name.startsWith("~$") || name.startsWith(".$");
    }

    private boolean shouldProcess(Path file) {
        try {
            if (!Files.isRegularFile(file) || Files.isSymbolicLink(file)) return false;

            String base = file.getFileName().toString();
            if (base.startsWith(".")) return false;
            try {
                if (Files.isHidden(file)) return false;
            } catch (IOException ignore) {
            }
            if (NAME_SKIP.contains(base)) return false;
            if (looksBackupOrTemp(base)) return false;

            int dot = base.lastIndexOf('.');
            if (dot > 0 && dot < base.length() - 1) {
                String ext = base.substring(dot + 1).toLowerCase();
                if (EXT_SKIP.contains(ext)) return false;
            }

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

    @Override
    public void reEncryptFiles(byte[] oldKeyOrNull, byte[] newKey, Path root) {
        final boolean noPwdMode = vaultSession.isOpendWithNoPassword();
        final boolean withKeyMode = vaultSession.isProtectionEnabled();

        log.info("[REENCRYPT] start: root='{}', mode: withKey={}, noPwd={}, oldKeyPresent={}, newKeyLen={}",
                root, withKeyMode, noPwdMode, (oldKeyOrNull != null), (newKey == null ? 0 : newKey.length));

        EncryptionService decryptor = (oldKeyOrNull != null) ? new EncryptionServiceImpl(oldKeyOrNull) : null;
        EncryptionService encryptor = new EncryptionServiceImpl(newKey);

        long total = 0, filteredOut = 0, skippedEnc = 0, skippedPlain = 0, errors = 0, reencOk = 0, encOk = 0;

        try (Stream<Path> files = Files.walk(root)) {
            for (Path file : (Iterable<Path>) files::iterator) {
                total++;
                if (!shouldProcess(file)) {
                    filteredOut++;
                    if (log.isDebugEnabled()) log.debug("[REENCRYPT] skip(filter) {}", file);
                    continue;
                }

                try {
                    String data = fileSystemService.readFile(file);

                    boolean looksEncrypted = looksEncrypted(data);
                    if (noPwdMode) {
                        if (looksEncrypted) {
                            skippedEnc++;
                            if (log.isDebugEnabled()) log.debug("[REENCRYPT] noPwd: keep .enc as-is {}", file);
                            continue;
                        }
                        String encrypted = encryptor.encrypt(data, null);
                        fileSystemService.write(getEncryptedPathForPlain(file), encrypted);
                        encOk++;
                        if (log.isDebugEnabled()) log.debug("[REENCRYPT] noPwd: PLAIN -> ENC {}", file);
                    } else {
                        if (!looksEncrypted) {
                            skippedPlain++;
                            if (log.isDebugEnabled()) log.debug("[REENCRYPT] withKey: PLAIN left as-is {}", file);
                            continue;
                        }
                        if (decryptor == null) {
                            skippedEnc++;
                            if (log.isDebugEnabled()) log.debug("[REENCRYPT] withKey: missing oldKey -> skip ENC {}", file);
                            continue;
                        }
                        String plain = decryptor.decrypt(data, null);
                        String reenc = encryptor.encrypt(plain, null);
                        fileSystemService.write(file, reenc);
                        reencOk++;
                        if (log.isDebugEnabled()) log.debug("[REENCRYPT] withKey: ENC(old)->ENC(new) {}", file);
                    }
                } catch (DecryptionException e) {
                    skippedEnc++;
                    if (log.isDebugEnabled())
                        log.debug("[REENCRYPT] decrypt fail (probably foreign enc) {}: {}", file, e.toString());
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

        log.info("[REENCRYPT] done: total={}, filteredOut={}, encOk(new)={}, reencOk={}, skippedEnc={}, skippedPlain={}, errors={}",
                total, filteredOut, encOk, reencOk, skippedEnc, skippedPlain, errors);
    }

    @Override
    public void decryptFiles(byte[] oldKey, Path root) {
        log.info("[DECRYPT] start: root='{}', keyLen={}", root, oldKey == null ? 0 : oldKey.length);
        if (oldKey == null) {
            log.info("[DECRYPT] no key -> nothing to decrypt");
            return;
        }
        EncryptionService decryptor = new EncryptionServiceImpl(oldKey);

        long total = 0, filteredOut = 0, wroteDec = 0, skipped = 0, errors = 0;
        try (Stream<Path> files = Files.walk(root)) {
            for (Path file : (Iterable<Path>) files::iterator) {
                total++;
                if (!shouldProcess(file)) { filteredOut++; continue; }

                try {
                    String data = fileSystemService.readFile(file);
                    if (!looksEncrypted(data)) {
                        skipped++;
                        if (log.isDebugEnabled()) log.debug("[DECRYPT] not our enc: {}", file);
                        continue;
                    }
                    String decrypted = decryptor.decrypt(data, null);
                    Path out = getDecryptedFilePath(file);
                    fileSystemService.write(out, decrypted);
                    wroteDec++;
                    if (log.isDebugEnabled()) log.debug("[DECRYPT] ENC -> {}", out);
                } catch (DecryptionException e) {
                    skipped++;
                    if (log.isDebugEnabled()) log.debug("[DECRYPT] decrypt fail (foreign?) {}: {}", file, e.toString());
                } catch (Exception e) {
                    errors++;
                    log.info("[DECRYPT] error {}: {}", file, e.toString());
                }
            }
        } catch (IOException e) {
            throw new CustomIOException(e.getMessage());
        }
        log.info("[DECRYPT] done: total={}, filteredOut={}, wroteDec={}, skipped={}, errors={}",
                total, filteredOut, wroteDec, skipped, errors);
    }

    @Override
    public void encryptFiles(byte[] newKey, Path root) {
        log.info("[ENCRYPT] start: root='{}', keyLen={}", root, newKey == null ? 0 : newKey.length);
        EncryptionService encryptor = new EncryptionServiceImpl(newKey);

        long total = 0, filteredOut = 0, encOk = 0, skippedEnc = 0, errors = 0;
        try (Stream<Path> files = Files.walk(root)) {
            for (Path file : (Iterable<Path>) files::iterator) {
                total++;
                if (!shouldProcess(file)) { filteredOut++; continue; }

                try {
                    String data = fileSystemService.readFile(file);
                    if (looksEncrypted(data)) {
                        skippedEnc++;
                        if (log.isDebugEnabled()) log.debug("[ENCRYPT] already enc (ours): {}", file);
                        continue;
                    }
                    String encrypted = encryptor.encrypt(data, null);
                    Path out = getEncryptedPathForPlain(file);
                    fileSystemService.write(out, encrypted);
                    encOk++;
                    if (log.isDebugEnabled()) log.debug("[ENCRYPT] PLAIN -> {}", out);
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
        log.info("[ENCRYPT] done: total={}, filteredOut={}, encOk={}, skippedEnc={}, errors={}",
                total, filteredOut, encOk, skippedEnc, errors);
    }

    private boolean looksEncrypted(String data) {
        return data != null && data.startsWith(EncryptionServiceImpl.HEADER);
    }

    private Path getDecryptedFilePath(Path path) {
        String base = path.getFileName().toString();
        if (base.endsWith(".enc")) base = base.substring(0, base.length() - 4);
        return getPathWithSibl(path, base + ".dec");
    }

    private Path getEncryptedFilePath(Path path) {
        return getPathWithSibl(path, path.getFileName().toString() + ".enc");
    }

    private Path getEncryptedPathForPlain(Path path) {
        // для открытого — писать рядом как filename.ext.enc
        return getPathWithSibl(path, path.getFileName().toString() + ".enc");
    }

    private static Path getPathWithSibl(Path path, String newName) {
        Path parent = path.getParent();
        return (parent == null) ? Path.of(newName) : parent.resolve(newName);
    }
}
