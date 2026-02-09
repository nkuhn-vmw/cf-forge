package com.cfforge.api.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.stereotype.Controller;

import java.security.Principal;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Controller
@Slf4j
public class CollaborationController {

    // Track active users per project
    private final Map<String, Map<String, UserPresence>> projectPresence = new ConcurrentHashMap<>();

    // --- Edit Operations ---

    @MessageMapping("/collab/{projectId}/edit")
    @SendTo("/topic/collab/{projectId}/edits")
    public EditOperation handleEdit(@DestinationVariable String projectId,
                                     EditOperation operation,
                                     Principal principal) {
        operation.setUserId(principal != null ? principal.getName() : "anonymous");
        operation.setTimestamp(Instant.now().toEpochMilli());
        log.debug("Edit from {} on project {} file {}", operation.getUserId(), projectId, operation.getFilePath());
        return operation;
    }

    // --- Cursor Tracking ---

    @MessageMapping("/collab/{projectId}/cursor")
    @SendTo("/topic/collab/{projectId}/cursors")
    public CursorPosition handleCursor(@DestinationVariable String projectId,
                                        CursorPosition position,
                                        Principal principal) {
        String userId = principal != null ? principal.getName() : "anonymous";
        position.setUserId(userId);
        return position;
    }

    // --- Presence ---

    @MessageMapping("/collab/{projectId}/join")
    @SendTo("/topic/collab/{projectId}/presence")
    public PresenceEvent handleJoin(@DestinationVariable String projectId,
                                     UserPresence presence,
                                     Principal principal) {
        String userId = principal != null ? principal.getName() : "anonymous";
        presence.setUserId(userId);
        presence.setJoinedAt(Instant.now().toEpochMilli());

        projectPresence.computeIfAbsent(projectId, k -> new ConcurrentHashMap<>())
            .put(userId, presence);

        log.info("User {} joined project {}", userId, projectId);
        return new PresenceEvent("join", userId, presence.getDisplayName(), presence.getColor());
    }

    @MessageMapping("/collab/{projectId}/leave")
    @SendTo("/topic/collab/{projectId}/presence")
    public PresenceEvent handleLeave(@DestinationVariable String projectId,
                                      Principal principal) {
        String userId = principal != null ? principal.getName() : "anonymous";
        var presenceMap = projectPresence.get(projectId);
        UserPresence presence = null;
        if (presenceMap != null) {
            presence = presenceMap.remove(userId);
        }

        log.info("User {} left project {}", userId, projectId);
        return new PresenceEvent("leave", userId,
            presence != null ? presence.getDisplayName() : userId, null);
    }

    // --- File Selection Broadcast ---

    @MessageMapping("/collab/{projectId}/file-select")
    @SendTo("/topic/collab/{projectId}/file-selections")
    public FileSelection handleFileSelect(@DestinationVariable String projectId,
                                           FileSelection selection,
                                           Principal principal) {
        selection.setUserId(principal != null ? principal.getName() : "anonymous");
        return selection;
    }

    // --- DTOs ---

    @lombok.Data
    public static class EditOperation {
        private String userId;
        private String filePath;
        private int startLine;
        private int startColumn;
        private int endLine;
        private int endColumn;
        private String text;
        private String type; // "insert", "delete", "replace"
        private long timestamp;
    }

    @lombok.Data
    public static class CursorPosition {
        private String userId;
        private String filePath;
        private int line;
        private int column;
        private String displayName;
        private String color;
    }

    @lombok.Data
    public static class UserPresence {
        private String userId;
        private String displayName;
        private String color;
        private long joinedAt;
    }

    public record PresenceEvent(String type, String userId, String displayName, String color) {}

    @lombok.Data
    public static class FileSelection {
        private String userId;
        private String filePath;
        private String displayName;
    }
}
