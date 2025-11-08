package org.example.tonpad.core.service.crypto.Impl;

import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Random;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import org.example.tonpad.core.service.crypto.EncryptionService;
import org.example.tonpad.core.service.crypto.exception.DecryptionException;
import org.example.tonpad.core.service.crypto.exception.EncryptionException;

import lombok.NonNull;
//Пароль пользователя прогони через эту хрень PBKDF2 (salt, iterations) -> AESключ 256 бит
public class EncryptionServiceImpl implements EncryptionService {

    private static final String KEY_LENGTH_ERROR = "key must be only 16/24/32 bytes length";
    
    private final static String ALGORYTHM = "AES";
    private final static String TRANSFORMATION = "AES/GSM/NoPadding";
    private final static int NONCE_LEN = 12;
    private final static int TAG_LEN_BITS  = 128;

    private final static Random rnd = new SecureRandom();

    private SecretKey key;
    
    public EncryptionServiceImpl(@NonNull byte[] key) {
        if(key.length != 16 || key.length != 24 || key.length != 32) throw new IllegalArgumentException(KEY_LENGTH_ERROR);
        this.key = new SecretKeySpec(key, ALGORYTHM);
    }

    @Override
    public String encrypt(String text, String aad) throws EncryptionException {
        return new String(encrypt(text.getBytes(StandardCharsets.UTF_8), aad == null ? null : aad.getBytes(StandardCharsets.UTF_8)), StandardCharsets.UTF_8);
    }

    @Override
    public byte[] encrypt(byte[] text, byte[] aad) throws EncryptionException {
        try{
            byte[] nonce = new byte[NONCE_LEN];
            rnd.nextBytes(nonce);
    
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.ENCRYPT_MODE, key, new GCMParameterSpec(TAG_LEN_BITS, nonce));
            if (aad != null) cipher.updateAAD(aad);

            byte[] cipherText = cipher.doFinal(text);
            byte[] output = new byte[NONCE_LEN + cipherText.length];
            System.arraycopy(nonce, 0, output, 0, NONCE_LEN);
            System.arraycopy(cipherText, 0, output, NONCE_LEN, cipherText.length);
            return output;
        }
        catch(Exception e) {
            throw new EncryptionException(e);
        }
    }

    @Override
    public String decrypt(String text, String aad) throws DecryptionException {
        return new String(decrypt(text.getBytes(StandardCharsets.UTF_8), aad == null ? null : aad.getBytes(StandardCharsets.UTF_8)), StandardCharsets.UTF_8);
    }

    @Override
    public byte[] decrypt(byte[] text, byte[] aad) throws DecryptionException {
        try {
            byte[] nonce = Arrays.copyOfRange(text, 0, NONCE_LEN);
            byte[] cipherText = Arrays.copyOfRange(text, NONCE_LEN, text.length);
    
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(cipher.DECRYPT_MODE, key, new GCMParameterSpec(TAG_LEN_BITS, nonce));
            if(aad != null) cipher.updateAAD(aad);
    
            return cipher.doFinal(cipherText);
        }
        catch(Exception e) {
            throw new DecryptionException(e);
        }
    }
}
