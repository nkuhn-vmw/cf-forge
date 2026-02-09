package com.cfforge.api;

import com.cfforge.api.config.SsoProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication
@ComponentScan(basePackages = {"com.cfforge.api", "com.cfforge.common"})
@EntityScan(basePackages = "com.cfforge.common.entity")
@EnableJpaRepositories(basePackages = "com.cfforge.common.repository")
@EnableConfigurationProperties(SsoProperties.class)
public class CfForgeApiApplication {
    public static void main(String[] args) {
        SpringApplication.run(CfForgeApiApplication.class, args);
    }
}
