package org.example.tonpad;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties({TonpadConfig.class})
public class Starter {

    public static void main(String[] args) {
        TonpadApplication.main(args);
    }

}
