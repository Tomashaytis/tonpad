package org.example.tonpad.core.service.crypto;

import org.example.tonpad.core.service.crypto.exception.DerivationException;

public interface DerivationService {
    byte[] deriveKey(char[] password, byte[] salt, int iterations, int keyLenBits, String algorythm) throws DerivationException;
    byte[] deriveAuthHash(char[] password, byte[] salt, int iterations) throws DerivationException;
    byte[] deriveAuthHash(char[] password) throws DerivationException;
    byte[] getSalt();
    boolean constantTimeEquals(byte[] a, byte[] b);
    int defaultIterations();
}
