package org.example.tonpad;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.nio.file.Path;

@ConfigurationProperties(prefix = "app", ignoreUnknownFields = false)
public record TonpadConfig(
        String dataPath,
        ReservedDirNames reservedNames,
        Path jsFunctionsDirectory
) {

    public record ReservedDirNames(String templatesDir, String notesDir) {}
}
