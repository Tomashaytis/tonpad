package org.example.tonpad.core.service.crypto;

public interface EncryptorFactory {
    Encryptor encryptorForKey(byte[] key);
    Encryptor encryptorForKey();
}
