package org.example.tonpad.core.session;

import java.util.Optional;

import javax.crypto.SecretKey;

import org.example.tonpad.core.service.crypto.exception.DerivationException;

public interface VaultSession {
    void unlock(char[] password) throws DerivationException;
    void openWithoutPassword();
    void lock();
    boolean isUnlocked();
    boolean isProtectionEnabled();
    boolean isOpendWithNoPassword();
    SecretKey requiredKey();
    Optional<SecretKey> getKeyIfPresent();
}
