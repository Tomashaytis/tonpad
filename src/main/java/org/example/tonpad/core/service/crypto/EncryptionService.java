package org.example.tonpad.core.service.crypto;

import java.nio.file.Path;

import org.example.tonpad.core.exceptions.DecryptionException;
import org.example.tonpad.core.exceptions.EncryptionException;

public interface EncryptionService {
    String encrypt(String text, String aad) throws EncryptionException;
    byte[] encrypt(byte[] text, byte[] aad) throws EncryptionException;
    String decrypt(String text, String aad) throws DecryptionException;
    byte[] decrypt(byte[] text, byte[] aad) throws DecryptionException;
    boolean isActionWithNoPasswordAllowed(Path path);
}
