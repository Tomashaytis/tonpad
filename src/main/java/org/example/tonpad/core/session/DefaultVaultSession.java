package org.example.tonpad.core.session;

import java.util.Arrays;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import org.example.tonpad.core.service.crypto.DerivationService;
import org.example.tonpad.core.service.crypto.exception.DerivationException;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;

import java.lang.reflect.Field;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

@Component
@Lazy
@RequiredArgsConstructor
public class DefaultVaultSession implements VaultSession {
    private final DerivationService derivationService;

    private enum Mode {LOCKED, UNLOCKED_NO_KEY, UNLOCKED_WITH_KEY};

    private final AtomicReference<SecretKey> masterKeyRef = new AtomicReference<>();

    private volatile Mode mode = Mode.LOCKED;

    @Override
    public void unlock(char[] password) throws DerivationException {
        try {
            if(mode != Mode.LOCKED) throw new IllegalStateException("vault is already opened");
            byte[] keyBytes = derivationService.deriveAuthHash(password, null, derivationService.defaultIterations());
            Arrays.fill(password, '\0');

            SecretKey key = new SecretKeySpec(keyBytes, "AES");
            Arrays.fill(keyBytes, (byte)0);
            if(!masterKeyRef.compareAndSet(null, key)) {
                zeroKey(key);
                throw new IllegalStateException("Vault already unlocked");
            }
            mode = Mode.UNLOCKED_WITH_KEY;
        }
        catch(DerivationException e) {
            throw e;
        }
    }

    @Override
    public void openWithoutPassword() {
        if(mode != Mode.LOCKED) throw new IllegalStateException("vault is already opened");
        SecretKey prev = masterKeyRef.getAndSet(null);
        if(prev != null) zeroKey(prev);
        mode = Mode.UNLOCKED_NO_KEY;
    }

    @Override
    public void lock() {
        SecretKey key = masterKeyRef.getAndSet(null);
        if(key != null) zeroKey(key);
        mode = Mode.LOCKED;
    }

    @Override
    public boolean isUnlocked() {
        return masterKeyRef.get() != null;
    }

    @Override
    public boolean isProtectionEnabled() {
        return mode == Mode.UNLOCKED_WITH_KEY;
    }

    @Override
    public Optional<SecretKey> getMasterKeyIfPresent() {
        return isProtectionEnabled() ? Optional.of(masterKeyRef.get()) : Optional.empty();
    }

    @Override
    public SecretKey requiredMasterKey() {
        if(!isProtectionEnabled()) throw new IllegalStateException("vault is not protected with password (or locked)");
        SecretKey key = masterKeyRef.get();
        return key;
    }

    private void zeroKey(SecretKey key) {
        if(key instanceof SecretKeySpec) {
            try {
                Field field = SecretKeySpec.class.getDeclaredField("key");
                field.setAccessible(true);
                byte[] array = (byte[]) field.get(key);
                if(array != null) Arrays.fill(array, (byte)0);
            }
            catch (Throwable e) {}
        }
    }
}
