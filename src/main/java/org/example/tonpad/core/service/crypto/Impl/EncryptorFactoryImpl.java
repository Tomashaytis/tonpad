package org.example.tonpad.core.service.crypto.Impl;

import org.example.tonpad.core.service.crypto.Encryptor;
import org.example.tonpad.core.service.crypto.EncryptorFactory;
import org.springframework.stereotype.Service;

@Service
public class EncryptorFactoryImpl implements EncryptorFactory {
    @Override
    public Encryptor encryptorForKey(byte[] key) {
        return new AesGcmEncryptor(key);
    }

    @Override
    public Encryptor encryptorForKey() {
        return new AesGcmEncryptor();
    }
}
