package com.cfforge.api.controller;

import com.cfforge.api.service.CfClient;
import com.cfforge.api.model.ServiceOffering;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/marketplace")
public class MarketplaceController {

    private final CfClient cfClient;

    public MarketplaceController(CfClient cfClient) {
        this.cfClient = cfClient;
    }

    @GetMapping("/services")
    public List<ServiceOffering> listServices() {
        return cfClient.listMarketplace().collectList().block();
    }

    @PostMapping("/services/{serviceName}/provision")
    public void provisionService(
            @PathVariable String serviceName,
            @RequestParam String plan,
            @RequestParam String instanceName,
            @RequestParam(required = false) String appGuid) {
        // Provision service and optionally bind to app
        // This delegates to CF API to create-service and bind-service
    }
}
