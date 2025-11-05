package org.example.tonpad.core.service.impl;

import org.example.tonpad.TonpadConfig;
import org.example.tonpad.core.exceptions.CustomIOException;
import org.example.tonpad.core.files.directory.DirectoryService;
import org.example.tonpad.core.service.VaultInitializerService;
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
import java.sql.Statement;

@Service
public class VaultInitializerServiceImpl implements VaultInitializerService {

    private static final String DATABASE_NAME = "database.db";

    private static final String DRIVER_STRING = "jdbc:sqlite:";

    private final DirectoryService directoryService;

    private final String changelogPath;


    @Autowired
    public VaultInitializerServiceImpl(DirectoryService directoryService, TonpadConfig config) {
        this.directoryService = directoryService;
        changelogPath = config.changelogPath();
    }

    @Override
    public void initVault(Path path) {
        directoryService.createDir(path, "notes");
        directoryService.createDir(path, "images");
        directoryService.createDir(path, "snippets");

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
            throw new CustomIOException("Произошла ошибка при инициализации волта");
        }
    }

    private String readScript(String changelogPath) throws IOException {
        Resource resource = new ClassPathResource(changelogPath);
        return StreamUtils.copyToString(resource.getInputStream(), StandardCharsets.UTF_8);
    }
}
