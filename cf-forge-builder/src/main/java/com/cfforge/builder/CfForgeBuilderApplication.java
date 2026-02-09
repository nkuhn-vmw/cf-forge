package com.cfforge.builder;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication
@EntityScan(basePackages = "com.cfforge.common.entity")
@EnableJpaRepositories(basePackages = "com.cfforge.common.repository")
public class CfForgeBuilderApplication {
    public static void main(String[] args) {
        SpringApplication.run(CfForgeBuilderApplication.class, args);
    }
}
