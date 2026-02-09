package com.cfforge.admin.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class AdminSecurityConfig {

    @Bean
    public SecurityFilterChain adminFilterChain(HttpSecurity http) throws Exception {
        return http
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/actuator/health", "/actuator/info").permitAll()
                .requestMatchers("/admin/**").authenticated()
                .requestMatchers("/api/v1/admin/**").authenticated()
                .anyRequest().denyAll()
            )
            .oauth2Login(oauth2 -> oauth2
                .defaultSuccessUrl("/admin/", true)
                .userInfoEndpoint(userInfo -> userInfo
                    .oidcUserService(cfForgeOidcUserService())
                )
            )
            .oauth2Client(Customizer.withDefaults())
            .logout(logout -> logout
                .logoutSuccessUrl("/admin/")
                .invalidateHttpSession(true)
            )
            .build();
    }

    @Bean
    public OidcUserService cfForgeOidcUserService() {
        return new OidcUserService() {
            @Override
            public OidcUser loadUser(OidcUserRequest userRequest) {
                OidcUser oidcUser = super.loadUser(userRequest);
                var scopes = userRequest.getAccessToken().getScopes();
                if (!scopes.contains("cfforge.admin")) {
                    throw new OAuth2AuthenticationException(
                        "Insufficient permissions for admin dashboard");
                }
                return oidcUser;
            }
        };
    }
}
