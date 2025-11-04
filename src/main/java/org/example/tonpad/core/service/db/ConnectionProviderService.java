package org.example.tonpad.core.service.db;

import org.jooq.DSLContext;

public interface ConnectionProviderService {

    DSLContext getDSLContext(String vaultPath);
}
