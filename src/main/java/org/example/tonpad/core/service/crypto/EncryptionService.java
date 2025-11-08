package org.example.tonpad.core.service.crypto;

import org.example.tonpad.core.service.crypto.exception.DecryptionException;
import org.example.tonpad.core.service.crypto.exception.EncryptionException;

public interface EncryptionService {
    String encrypt(String text, String aad) throws EncryptionException;
    byte[] encrypt(byte[] text, byte[] aad) throws EncryptionException;
    String decrypt(String text, String aad) throws DecryptionException;
    byte[] decrypt(byte[] text, byte[] aad) throws DecryptionException;
}
