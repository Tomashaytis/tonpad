package org.example.tonpad.core.service.db.impl;

import com.zaxxer.hikari.HikariDataSource;
import org.example.tonpad.core.service.db.ConnectionProviderService;
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
public class ConnectionProviderServiceImpl implements ConnectionProviderService {

    private static final String DATABASE_NAME = "database.db";

    private static final String DRIVER_STRING = "jdbc:sqlite:";

    private final Map<String, DSLContext> contextMap = new HashMap<>();

    @Override
    public DSLContext getDSLContext(String vaultPath) {
        return contextMap.computeIfAbsent(vaultPath, this::createDSLContext);
    }

    private DSLContext createDSLContext(String vaultPath) {
        HikariDataSource ds = new HikariDataSource();
        ds.setJdbcUrl(DRIVER_STRING + Path.of(vaultPath, DATABASE_NAME));
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
