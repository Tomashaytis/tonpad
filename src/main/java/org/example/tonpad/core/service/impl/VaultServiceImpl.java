package org.example.tonpad.core.service.impl;

import org.example.tonpad.TonpadConfig;
import org.example.tonpad.core.session.RecentTabsConfig;
import org.example.tonpad.core.exceptions.CustomIOException;
import org.example.tonpad.core.files.FileSystemService;
import org.example.tonpad.core.files.directory.DirectoryService;
import org.example.tonpad.core.service.VaultService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.util.StreamUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

@Service
public class VaultServiceImpl implements VaultService {

    private static final String DATABASE_NAME = "database.db";

    private static final String DRIVER_STRING = "jdbc:sqlite:";

    private final DirectoryService directoryService;

    private final FileSystemService fileSystemService;

    private final String changelogPath;

    private final String validateDbSchemaPath;

    @Autowired
    public VaultServiceImpl(DirectoryService directoryService, TonpadConfig config, FileSystemService fileSystemService) {
        this.directoryService = directoryService;
        changelogPath = config.changelogPath();
        validateDbSchemaPath = config.validateDbSchemaPath();
        this.fileSystemService = fileSystemService;
    }

    @Override
    public void initVault(Path path) {
        directoryService.createDir(path, "notes");
        directoryService.createDir(path, "images");
        directoryService.createDir(path, "snippets");
        fileSystemService.makeFile(path.resolve(RecentTabsConfig.getRtConfigName()));
        

        String url = DRIVER_STRING + path.resolve(DATABASE_NAME);
        try (Connection conn = DriverManager.getConnection(url);
             Statement stmt = conn.createStatement()) {

            String sql = readScript(changelogPath);

            for (String command : sql.split(";")) {
                command = command.trim();
                if (!command.isEmpty()) {
                    stmt.execute(command);
                }
            }
        } catch (Exception e) {
            throw new CustomIOException("Vault could not be initialized");
        }

        checkVaultInitialization(path);
    }

    @Override
    public void checkVaultInitialization(Path path) {
        directoryService.exists(path.resolve("notes"));
        directoryService.exists(path.resolve("images"));
        directoryService.exists(path.resolve("snippets"));
        directoryService.exists(path.resolve("database.db"));
        fileSystemService.exists(path.resolve(RecentTabsConfig.getRtConfigName()));

        String url = DRIVER_STRING + path.resolve(DATABASE_NAME);
        try (Connection conn = DriverManager.getConnection(url);
             Statement stmt = conn.createStatement()) {
            String sql = readScript(validateDbSchemaPath);
            ResultSet result = stmt.executeQuery(sql);

            List<String> errors = new ArrayList<>();
            while (result.next()) {
                String error = result.getString("error");
                if (error != null && !error.isEmpty()) {
                    errors.add(error);
                }
            }

            if (!errors.isEmpty()) {
                throw new SQLException(
                        "Schema validation failed:\n  - " +
                                String.join("\n  - ", errors)
                );
            }
        } catch (Exception e) {
            throw new CustomIOException("Vault is not initialized");
        }
    }

    private String readScript(String changelogPath) throws IOException {
        Resource resource = new ClassPathResource(changelogPath);
        return StreamUtils.copyToString(resource.getInputStream(), StandardCharsets.UTF_8);
    }
}
