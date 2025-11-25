package org.example.tonpad.core.service.db;

import org.jooq.DSLContext;

import java.nio.file.Path;

public interface ConnectionProviderService {

    DSLContext getDSLContext(Path vaultPath);

    DSLContext getDSLContext();
}
