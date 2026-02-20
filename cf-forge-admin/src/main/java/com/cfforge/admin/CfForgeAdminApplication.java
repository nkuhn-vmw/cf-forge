package com.cfforge.admin;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableScheduling;

import com.cfforge.admin.config.HealthCheckProperties;

@SpringBootApplication
@EntityScan(basePackages = "com.cfforge.common.entity")
@EnableJpaRepositories(basePackages = "com.cfforge.common.repository")
@EnableScheduling
@EnableConfigurationProperties(HealthCheckProperties.class)
@ComponentScan(basePackages = {"com.cfforge.admin", "com.cfforge.common"})
public class CfForgeAdminApplication {
    public static void main(String[] args) {
        SpringApplication.run(CfForgeAdminApplication.class, args);
    }
}
