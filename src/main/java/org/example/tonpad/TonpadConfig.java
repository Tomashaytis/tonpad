package org.example.tonpad;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app", ignoreUnknownFields = false)
public record TonpadConfig(
        String dataPath
) {
}
