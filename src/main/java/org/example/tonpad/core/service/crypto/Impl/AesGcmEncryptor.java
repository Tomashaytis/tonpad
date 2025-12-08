package org.example.tonpad.core.service.crypto.Impl;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;
import java.util.Random;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import org.example.tonpad.core.service.crypto.Encryptor;
import org.example.tonpad.core.exceptions.DecryptionException;
import org.example.tonpad.core.exceptions.EncryptionException;

import lombok.NonNull;

public class AesGcmEncryptor implements Encryptor {

    private static final String KEY_LENGTH_ERROR = "key must be only 16/24/32 bytes length";

    private static final String ALGORYTHM = "AES";
    private static final String TRANSFORMATION = "AES/GCM/NoPadding";
    private static final int NONCE_LEN = 12;
    private static final int TAG_LEN_BITS  = 128;

    private static final Random rnd = new SecureRandom();

    public static final String HEADER = "TOP::LARSENS::INC::LTD::WOODLANE::LONDON::ASTON::VANQUISH::V12\\n";
    private static final byte[] HEADER_BYTES = HEADER.getBytes(StandardCharsets.US_ASCII);

    private final SecretKey key;

    public AesGcmEncryptor()
    {
        this.key = null;
    }

    public AesGcmEncryptor(@NonNull byte[] key) {
        if (key.length != 16 && key.length != 24 && key.length != 32) throw new IllegalArgumentException(KEY_LENGTH_ERROR);
        this.key = new SecretKeySpec(key, ALGORYTHM);
    }

    @Override
    public String encrypt(String text, String aad) {
        byte[] plain = text.getBytes(StandardCharsets.UTF_8);
        byte[] aadBytes = aad == null ? null : aad.getBytes(StandardCharsets.UTF_8);
        byte[] wrapped = encrypt(plain, aadBytes);
        return new String(wrapped, StandardCharsets.UTF_8);
    }

    @Override
    public byte[] encrypt(byte[] text, byte[] aad) {
        try {
            byte[] nonce = new byte[NONCE_LEN];
            rnd.nextBytes(nonce);

            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.ENCRYPT_MODE, key, new GCMParameterSpec(TAG_LEN_BITS, nonce));
            if (aad != null && aad.length > 0) cipher.updateAAD(aad);

            byte[] cipherText = cipher.doFinal(text);
            byte[] output = new byte[NONCE_LEN + cipherText.length];
            System.arraycopy(nonce, 0, output, 0, NONCE_LEN);
            System.arraycopy(cipherText, 0, output, NONCE_LEN, cipherText.length);

            String base64 = Base64.getEncoder().encodeToString(output);
            String wrapped = HEADER + base64;
            return wrapped.getBytes(StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new EncryptionException("Encryption error", e);
        }
    }

    @Override
    public String decrypt(String text, String aad) {
        byte[] in = text.getBytes(StandardCharsets.UTF_8);
        byte[] aadBytes = aad == null ? null : aad.getBytes(StandardCharsets.UTF_8);
        byte[] out = decrypt(in, aadBytes);
        return new String(out, StandardCharsets.UTF_8);
    }

    @Override
    public byte[] decrypt(byte[] text, byte[] aad) {
        if (!startWith(text, HEADER_BYTES)) return text;
        try {
            String whole = new String(text, StandardCharsets.UTF_8);
            if (!whole.startsWith(HEADER)) return text; // не наш формат
            String base64 = whole.substring(HEADER.length()).trim();
            byte[] packed = Base64.getDecoder().decode(base64);

            if (packed.length < NONCE_LEN + 1) throw new DecryptionException("Invalid payload length");

            byte[] nonce = Arrays.copyOfRange(packed, 0, NONCE_LEN);
            byte[] cipherText = Arrays.copyOfRange(packed, NONCE_LEN, packed.length);

            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(TAG_LEN_BITS, nonce));
            if (aad != null && aad.length > 0) cipher.updateAAD(aad);

            return cipher.doFinal(cipherText);
        } catch (Exception e) {
            throw new DecryptionException("Decryption error", e);
        }
    }

    @Override
    public boolean isActionWithNoPasswordAllowed(Path path)
    {
        try {
            if (!(Files.newInputStream(path).readNBytes(HEADER_BYTES.length).equals(HEADER_BYTES)))
            {
                return false;
            }
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    private static boolean startWith(byte[] data, byte[] prefix) {
        if (data.length < prefix.length) return false;
        for (int i = 0; i < prefix.length; i++) {
            if (data[i] != prefix[i]) return false;
        }
        return true;
    }
}
