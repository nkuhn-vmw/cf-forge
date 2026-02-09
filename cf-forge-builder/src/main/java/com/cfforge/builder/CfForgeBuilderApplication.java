package com.cfforge.builder;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication
@EntityScan(basePackages = "com.cfforge.common.entity")
@EnableJpaRepositories(basePackages = "com.cfforge.common.repository")
@ComponentScan(basePackages = {"com.cfforge.builder", "com.cfforge.common"})
public class CfForgeBuilderApplication {
    public static void main(String[] args) {
        SpringApplication.run(CfForgeBuilderApplication.class, args);
    }
}
