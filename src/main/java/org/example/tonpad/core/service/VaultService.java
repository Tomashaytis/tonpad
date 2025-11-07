package org.example.tonpad.core.service;

import java.nio.file.Path;

public interface VaultService {

    void initVault(Path path);

    void checkVaultInitialization(Path path);
}
