package org.example.tonpad.core.session;

import javax.crypto.SecretKey;

import org.example.tonpad.core.service.crypto.exception.DerivationException;

public interface VaultSession {
    void unlock(char[] password) throws DerivationException;
    void lock();
    boolean isUnlocked();
    SecretKey requiredMasterKey();
}
