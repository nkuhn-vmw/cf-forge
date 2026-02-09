package com.cfforge.api.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.stereotype.Controller;

import java.security.Principal;
import java.time.Instant;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Shared terminal sessions via WebSocket STOMP.
 * Multiple users can observe and interact with the same terminal session.
 */
@Controller
@Slf4j
public class SharedTerminalController {

    // Track which users are connected to each terminal session
    private final Map<String, Set<String>> sessionParticipants = new ConcurrentHashMap<>();

    @MessageMapping("/terminal/{sessionId}/input")
    @SendTo("/topic/terminal/{sessionId}/output")
    public TerminalMessage handleInput(@DestinationVariable String sessionId,
                                        TerminalInput input,
                                        Principal principal) {
        String userId = principal != null ? principal.getName() : "anonymous";
        log.debug("Terminal input from {} on session {}", userId, sessionId);

        return new TerminalMessage(
            userId,
            input.getData(),
            "input",
            Instant.now().toEpochMilli()
        );
    }

    @MessageMapping("/terminal/{sessionId}/resize")
    @SendTo("/topic/terminal/{sessionId}/resize")
    public TerminalResize handleResize(@DestinationVariable String sessionId,
                                        TerminalResize resize,
                                        Principal principal) {
        resize.setUserId(principal != null ? principal.getName() : "anonymous");
        return resize;
    }

    @MessageMapping("/terminal/{sessionId}/join")
    @SendTo("/topic/terminal/{sessionId}/participants")
    public ParticipantEvent handleJoinTerminal(@DestinationVariable String sessionId,
                                                Principal principal) {
        String userId = principal != null ? principal.getName() : "anonymous";
        sessionParticipants.computeIfAbsent(sessionId, k -> ConcurrentHashMap.newKeySet())
            .add(userId);

        log.info("User {} joined terminal session {}", userId, sessionId);
        return new ParticipantEvent("join", userId,
            sessionParticipants.get(sessionId).size());
    }

    @MessageMapping("/terminal/{sessionId}/leave")
    @SendTo("/topic/terminal/{sessionId}/participants")
    public ParticipantEvent handleLeaveTerminal(@DestinationVariable String sessionId,
                                                 Principal principal) {
        String userId = principal != null ? principal.getName() : "anonymous";
        var participants = sessionParticipants.get(sessionId);
        if (participants != null) {
            participants.remove(userId);
        }

        log.info("User {} left terminal session {}", userId, sessionId);
        int count = participants != null ? participants.size() : 0;
        return new ParticipantEvent("leave", userId, count);
    }

    // --- DTOs ---

    @lombok.Data
    public static class TerminalInput {
        private String data;
    }

    public record TerminalMessage(String userId, String data, String type, long timestamp) {}

    @lombok.Data
    public static class TerminalResize {
        private String userId;
        private int cols;
        private int rows;
    }

    public record ParticipantEvent(String type, String userId, int participantCount) {}
}
