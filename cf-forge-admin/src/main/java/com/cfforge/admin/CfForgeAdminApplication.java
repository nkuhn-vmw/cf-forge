package com.cfforge.admin;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EntityScan(basePackages = "com.cfforge.common.entity")
@EnableJpaRepositories(basePackages = "com.cfforge.common.repository")
@EnableScheduling
public class CfForgeAdminApplication {
    public static void main(String[] args) {
        SpringApplication.run(CfForgeAdminApplication.class, args);
    }
}
