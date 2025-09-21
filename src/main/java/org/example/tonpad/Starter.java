package org.example.tonpad;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;

@SpringBootApplication
public class Starter {

    public static void main(String[] args) {
        new SpringApplicationBuilder(Starter.class).run();
        TonpadApplication.main(args);
    }

}
