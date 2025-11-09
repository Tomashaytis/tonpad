package org.example.tonpad.core.service.crypto.Impl;

import java.security.SecureRandom;
import java.util.Random;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

import org.example.tonpad.core.service.crypto.DerivationService;
import org.example.tonpad.core.service.crypto.exception.DerivationException;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class DerivationServiceImpl implements DerivationService {
    private static final String KDF = "PBKDF2WithHmacSHA256";
    private static final byte[] SALT = hex("40e0bbd7ba19094abf81cc4b320fba1f");
    private static final int iterations = 500_000;
    private static final int KEY_LENGTH_BITS = 256;
    private static final Random rnd = new SecureRandom();

    @Override
    public byte[] deriveKey(char[] password, byte[] salt, int iterations, int keyLenBits, String algorythm) throws DerivationException {
        try {
            byte[] kdfSalt = salt == null ? getSalt() : salt;
            PBEKeySpec spec = new PBEKeySpec(password, kdfSalt, iterations, keyLenBits);
            SecretKeyFactory keyFactory = SecretKeyFactory.getInstance(algorythm);
            byte[] key = keyFactory.generateSecret(spec).getEncoded();
            spec.clearPassword();
            return key;
        }
        catch (Exception e) {
            throw new DerivationException(e);
        }
    }

    @Override
    public byte[] deriveAuthHash(char[] password, byte[] salt, int iterations) throws DerivationException {
        return deriveKey(password, salt, iterations, KEY_LENGTH_BITS, KDF);
    }

    @Override
    public byte[] deriveAuthHash(char[] password) throws DerivationException {
        return deriveAuthHash(password, getSalt(), defaultIterations());
    }

    @Override
    public byte[] getSalt() {
        return SALT.clone();
    }

    private static byte[] hex(String text) {
        int len = text.length();
        byte[] output = new byte[len/2];
        for(int i = 0; i < len; i += 2) {
            output[i/2] = (byte) Integer.parseInt(text.substring(i, i + 2), 16);
        }
        return output;
    }

    // чтоб по времени нельзя было понять, где примерно различаются массивы
    @Override
    public boolean constantTimeEquals(byte[] a, byte[] b) {
        if(a == null || b == null || a.length != b.length) return false;
        int result = 0;
        for(int i = 0; i < a.length; i++) result |= (a[i] ^ b[i]);
        return result == 0;
    }

    @Override
    public int defaultIterations() {
        return iterations;
    }
}
