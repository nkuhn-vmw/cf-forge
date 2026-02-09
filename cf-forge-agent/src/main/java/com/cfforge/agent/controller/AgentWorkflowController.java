package com.cfforge.agent.controller;

import com.cfforge.agent.agents.AgentOrchestrator;
import com.cfforge.agent.agents.AgentOrchestrator.*;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * REST API for multi-agent workflows.
 */
@RestController
@RequestMapping("/api/v1/agents")
public class AgentWorkflowController {

    private final AgentOrchestrator orchestrator;

    public AgentWorkflowController(AgentOrchestrator orchestrator) {
        this.orchestrator = orchestrator;
    }

    @GetMapping
    public Collection<AgentDefinition> listAgents() {
        return orchestrator.listAgents();
    }

    @PostMapping("/workflow")
    public AgentWorkflowResult executeWorkflow(@RequestBody Map<String, Object> request) {
        @SuppressWarnings("unchecked")
        List<String> agents = (List<String>) request.getOrDefault("agents",
            List.of("architect", "developer", "reviewer"));
        return orchestrator.executeWorkflow(
            (String) request.get("task"),
            (String) request.getOrDefault("context", ""),
            agents
        );
    }

    @PostMapping(value = "/{agentName}/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> streamAgent(@PathVariable String agentName,
                                      @RequestBody Map<String, String> request) {
        return orchestrator.streamAgent(
            agentName,
            request.get("message"),
            request.getOrDefault("context", "")
        );
    }
}
