package org.example.tonpad.core.files;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
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


    // если без пароля - то зашифровать
    @Override
    public void reEncryptFiles(byte[] oldKey, byte[] newKey, Path root) {
        EncryptionService decryptor = new EncryptionServiceImpl(oldKey);
        EncryptionService encryptor = new EncryptionServiceImpl(newKey);
        try(Stream<Path> files = Files.walk(root)) {
            files.forEach(file -> {
                try {
                    String data = fileSystemService.readFile(file);
                    String toEncrypt;
                    if(vaultSession.isOpendWithNoPassword()) toEncrypt = data;
                    else toEncrypt = decryptor.decrypt(data, null);
                    String encrypted = encryptor.encrypt(toEncrypt, null);
                    fileSystemService.write(file, encrypted);
                }
                catch(DecryptionException | EncryptionException e) {}
            });
        }
        catch(IOException e) {
            log.info(e.getMessage());
            throw new CustomIOException(e.getMessage());
        }
    }

    @Override
    public void decryptFiles(byte[] oldKey, Path root) {
        EncryptionService decryptor = new EncryptionServiceImpl(oldKey);
        try(Stream<Path> files = Files.walk(root)) {
            files.forEach(file -> {
                try {
                    String data = fileSystemService.readFile(file);
                    String decrypted = decryptor.decrypt(data, null);
                    fileSystemService.write(getDecryptedFilePath(file), decrypted);
                }
                catch(DecryptionException e) {}
            });
        }
        catch(IOException e) {
            log.info(e.getMessage());
            throw new CustomIOException(e.getMessage());
        }
    }

    private Path getDecryptedFilePath(Path path) {
        String filePath = path.getFileName().toString();
        if(filePath.endsWith(".enc")) filePath = filePath.substring(0, filePath.length() - 4);
        return Path.of(filePath + ".dec");
    }

    @Override
    public void encryptFiles(byte[] newKey, Path root) {
        EncryptionService encryptor = new EncryptionServiceImpl(newKey);

        try(Stream<Path> files = Files.walk(root)) {
            files.forEach(file -> {
                try{
                    String data = fileSystemService.readFile(file);
                    String encrypted = encryptor.encrypt(data, null);
                    fileSystemService.write(getEncryptedFilePath(file), encrypted);
                }
                catch(EncryptionException e) {}
            });
        }
        catch(IOException e) {
            log.info(e.getMessage());
            throw new CustomIOException(e.getMessage());
        }
    }

    private Path getEncryptedFilePath(Path path) {
        return Path.of(path.getFileName().toString() + ".enc");
    }
}
