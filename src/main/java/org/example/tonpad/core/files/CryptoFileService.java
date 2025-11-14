package org.example.tonpad.core.files;

import java.nio.file.Path;

public interface CryptoFileService {
    void reEncryptFiles(byte[] oldKey, byte[] newKey, Path root);
    void decryptFiles(byte[] oldKey, Path root);
    void encryptFiles(byte[] newKey, Path root);
}
