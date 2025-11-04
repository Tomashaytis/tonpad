package org.example.tonpad.core.service.db.impl;

import com.zaxxer.hikari.HikariDataSource;
import lombok.RequiredArgsConstructor;
import org.example.tonpad.core.service.db.ConnectionProviderService;
import org.example.tonpad.ui.extentions.VaultPath;
import org.jooq.Configuration;
import org.jooq.ConnectionProvider;
import org.jooq.DSLContext;
import org.jooq.SQLDialect;
import org.jooq.conf.Settings;
import org.jooq.impl.DSL;
import org.jooq.impl.DataSourceConnectionProvider;
import org.jooq.impl.DefaultConfiguration;
import org.springframework.stereotype.Service;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ConnectionProviderServiceImpl implements ConnectionProviderService {

    private static final String DATABASE_NAME = "database.db";

    private static final String DRIVER_STRING = "jdbc:sqlite:";

    private final Map<String, DSLContext> contextMap = new HashMap<>();

    private final VaultPath path;

    @Override
    public DSLContext getDSLContext() {
        return getDSLContext(path.getVaultPath());
    }

    @Override
    public DSLContext getDSLContext(String vaultPath) {
        return contextMap.computeIfAbsent(vaultPath, key -> createDSLContext());
    }

    private DSLContext createDSLContext() {
        HikariDataSource ds = new HikariDataSource();
        ds.setJdbcUrl(DRIVER_STRING + Path.of(path.getVaultPath(), DATABASE_NAME));
        ds.setDriverClassName("org.sqlite.JDBC");

        ConnectionProvider connectionProvider = new DataSourceConnectionProvider(ds);

        Settings settings = new Settings().withRenderSchema(false);

        Configuration configuration = new DefaultConfiguration()
                .derive(connectionProvider)
                .derive(SQLDialect.SQLITE)
                .derive(settings);

        return DSL.using(configuration);
    }
}
