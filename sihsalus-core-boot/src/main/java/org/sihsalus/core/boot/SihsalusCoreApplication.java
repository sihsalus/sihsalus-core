package org.sihsalus.core.boot;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.elasticsearch.ElasticsearchClientAutoConfiguration;
import org.springframework.boot.autoconfigure.elasticsearch.ElasticsearchRestClientAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication(
        exclude = {
            HibernateJpaAutoConfiguration.class,
            ElasticsearchClientAutoConfiguration.class,
            ElasticsearchRestClientAutoConfiguration.class
        })
@ComponentScan(basePackages = "org.sihsalus")
public class SihsalusCoreApplication {

    public static void main(String[] args) {
        SpringApplication.run(SihsalusCoreApplication.class, args);
    }
}
