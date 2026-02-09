package com.cfforge.api.service;

import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Set;

import static java.util.Map.entry;

@Service
public class CfRbacService {

    private final CfClient cfClient;

    private static final Map<String, Set<String>> PERMISSIONS = Map.ofEntries(
        entry("project.create",    Set.of("SpaceDeveloper", "SpaceManager", "OrgManager")),
        entry("project.edit",      Set.of("SpaceDeveloper", "SpaceManager", "OrgManager")),
        entry("project.view",      Set.of("SpaceDeveloper", "SpaceManager", "SpaceAuditor", "OrgManager")),
        entry("project.delete",    Set.of("SpaceDeveloper", "OrgManager")),
        entry("deploy.staging",    Set.of("SpaceDeveloper", "SpaceManager", "OrgManager")),
        entry("deploy.production", Set.of("SpaceDeveloper", "OrgManager")),
        entry("service.create",    Set.of("SpaceDeveloper", "OrgManager")),
        entry("service.bind",      Set.of("SpaceDeveloper", "OrgManager")),
        entry("audit.view",        Set.of("SpaceManager", "SpaceAuditor", "OrgManager")),
        entry("admin.access",      Set.of("OrgManager"))
    );

    public CfRbacService(CfClient cfClient) {
        this.cfClient = cfClient;
    }

    public boolean hasPermission(String uaaToken, String action, String spaceGuid) {
        Set<String> userRoles = cfClient.getUserSpaceRoles(uaaToken, spaceGuid);
        Set<String> requiredRoles = PERMISSIONS.getOrDefault(action, Set.of());
        return userRoles.stream().anyMatch(requiredRoles::contains);
    }
}
