package com.cfforge.api.service;

import com.cfforge.api.config.SsoProperties;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.Map;

@Service
public class OidcDiscoveryService {

    private static final Logger log = LoggerFactory.getLogger(OidcDiscoveryService.class);

    private final SsoProperties ssoProperties;
    private final RestClient restClient;

    private String authorizationEndpoint;
    private String tokenEndpoint;
    private String endSessionEndpoint;
    private String jwksUri;
    private String issuer;

    public OidcDiscoveryService(SsoProperties ssoProperties) {
        this.ssoProperties = ssoProperties;
        this.restClient = RestClient.create();
    }

    @PostConstruct
    public void discover() {
        String issuer = ssoProperties.issuerUri();
        // UAA issuer URIs often end with /oauth/token — strip to base URL
        String baseUri = issuer.replaceAll("/oauth/token$", "");

        String discoveryUrl = baseUri + "/.well-known/openid-configuration";
        log.info("Fetching OIDC discovery from {}", discoveryUrl);

        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> config = restClient.get()
                .uri(discoveryUrl)
                .retrieve()
                .body(Map.class);

            if (config != null) {
                this.authorizationEndpoint = (String) config.get("authorization_endpoint");
                this.tokenEndpoint = (String) config.get("token_endpoint");
                this.endSessionEndpoint = (String) config.get("end_session_endpoint");
                this.jwksUri = (String) config.get("jwks_uri");
                this.issuer = (String) config.get("issuer");
                log.info("OIDC discovery complete: auth={}, token={}, jwks={}, issuer={}",
                    authorizationEndpoint, tokenEndpoint, jwksUri, issuer);
            }
        } catch (Exception e) {
            log.warn("OIDC discovery failed from {}. Falling back to convention-based endpoints.", discoveryUrl, e);
            this.authorizationEndpoint = baseUri + "/oauth/authorize";
            this.tokenEndpoint = baseUri + "/oauth/token";
            this.endSessionEndpoint = baseUri + "/logout";
        }
    }

    public String getAuthorizationEndpoint() {
        return authorizationEndpoint;
    }

    public String getTokenEndpoint() {
        return tokenEndpoint;
    }

    public String getEndSessionEndpoint() {
        return endSessionEndpoint;
    }

    public String getJwksUri() {
        return jwksUri;
    }

    public String getIssuer() {
        return issuer;
    }
}
