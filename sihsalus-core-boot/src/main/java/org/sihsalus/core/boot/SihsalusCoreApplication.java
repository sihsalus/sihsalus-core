package org.sihsalus.core.boot;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@ComponentScan(basePackages = "org.sihsalus")
public class SihsalusCoreApplication {

    public static void main(String[] args) {
        SpringApplication.run(SihsalusCoreApplication.class, args);
    }
}
