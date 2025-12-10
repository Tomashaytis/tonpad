package org.example.tonpad.core.session;

import java.lang.reflect.Field;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.HexFormat;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import org.example.tonpad.core.exceptions.FingerPrintException;
import org.example.tonpad.core.service.crypto.DerivationService;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;

@Component
@Lazy
@RequiredArgsConstructor
public class DefaultVaultSession implements VaultSession {
    private final static String BAD_KEY_ERROR = "key is null or empty";
    private final static String GUEST_FINGER_PRINT = "guest";

    private final DerivationService derivationService;

    private enum Mode { LOCKED, UNLOCKED_NO_KEY, UNLOCKED_WITH_KEY }

    private final AtomicReference<SecretKey> keyRef = new AtomicReference<>();
    private volatile Mode mode = Mode.LOCKED;

    @Override
    public void unlock(char[] password) {
        if (password == null || password.length == 0) throw new IllegalArgumentException("empty password");
        try {
            byte[] keyBytes = derivationService.deriveAuthHash(password, null, derivationService.defaultIterations());
            Arrays.fill(password, '\0');

            SecretKey newKey = new SecretKeySpec(keyBytes, "AES");
            Arrays.fill(keyBytes, (byte) 0);

            switch (mode) {
                case LOCKED -> {
                    if (!keyRef.compareAndSet(null, newKey)) {
                        zeroKey(newKey);
                        throw new IllegalStateException("vault already unlocked");
                    }
                    mode = Mode.UNLOCKED_WITH_KEY;
                }
                case UNLOCKED_NO_KEY -> {
                    SecretKey prev = keyRef.getAndSet(newKey);
                    if (prev != null) zeroKey(prev);
                    mode = Mode.UNLOCKED_WITH_KEY;
                }
                case UNLOCKED_WITH_KEY -> {
                    SecretKey cur = keyRef.get();
                    if (cur == null) {
                        if (!keyRef.compareAndSet(null, newKey)) {
                            zeroKey(newKey);
                            throw new IllegalStateException("vault already unlocked");
                        }
                        mode = Mode.UNLOCKED_WITH_KEY;
                    } else {
                        if (!keysEqualConstantTime(cur, newKey)) {
                            zeroKey(newKey);
                            throw new IllegalStateException("vault is already opened with another key");
                        }
                        zeroKey(newKey);
                    }
                }
            }
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    @Override
    public void openWithoutPassword() {
        if (mode == Mode.UNLOCKED_WITH_KEY) {
            return;
        }
        SecretKey prev = keyRef.getAndSet(null);
        if (prev != null) zeroKey(prev);
        mode = Mode.UNLOCKED_NO_KEY;
    }

    @Override
    public void lock() {
        SecretKey key = keyRef.getAndSet(null);
        if (key != null) zeroKey(key);
        mode = Mode.LOCKED;
    }

    @Override
    public boolean isUnlocked() {
        return mode != Mode.LOCKED;
    }

    @Override
    public boolean isProtectionEnabled() {
        return mode == Mode.UNLOCKED_WITH_KEY;
    }

    @Override
    public boolean isOpendWithNoPassword() {
        return mode == Mode.UNLOCKED_NO_KEY;
    }

    @Override
    public Optional<SecretKey> getKeyIfPresent() {
        return isProtectionEnabled() ? Optional.ofNullable(keyRef.get()) : Optional.empty();
    }

    @Override
    public SecretKey requiredKey() {
        if (!isProtectionEnabled()) throw new IllegalStateException("vault is not protected with password (or locked)");
        return keyRef.get();
    }

    private void zeroKey(SecretKey key) {
        if (key instanceof SecretKeySpec) {
            try {
                Field f = SecretKeySpec.class.getDeclaredField("key");
                f.setAccessible(true);
                byte[] arr = (byte[]) f.get(key);
                if (arr != null) Arrays.fill(arr, (byte) 0);
            } catch (Throwable ignore) {}
        }
    }

    private boolean keysEqualConstantTime(SecretKey a, SecretKey b) {
        if (a == b) return true;
        if (a == null || b == null) return false;
        byte[] ka = a.getEncoded();
        byte[] kb = b.getEncoded();
        if (ka == null || kb == null || ka.length != kb.length) return false;
        int r = 0;
        for (int i = 0; i < ka.length; i++) r |= (ka[i] ^ kb[i]);
        Arrays.fill(ka, (byte) 0);
        Arrays.fill(kb, (byte) 0);
        return r == 0;
    }

    @Override
    public String getFingerPrint() {
        Optional<SecretKey> keyOpt = getKeyIfPresent();
        try {
            return keyOpt.isPresent() ? getFingerprintFromKey(keyOpt.get()) : GUEST_FINGER_PRINT;
        }
        catch (IllegalStateException ex) {
            throw new FingerPrintException(ex);
        }
    }

    private String getFingerprintFromKey(SecretKey key) {
        byte[] bytes = key.getEncoded();
        if (bytes == null || bytes.length == 0) throw new IllegalStateException(BAD_KEY_ERROR);
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(bytes);
            return HexFormat.of().withUpperCase().formatHex(digest);
        }
        catch (Exception ex) {
            throw new IllegalStateException("can't compute fingerprint");
        }
    }
}
