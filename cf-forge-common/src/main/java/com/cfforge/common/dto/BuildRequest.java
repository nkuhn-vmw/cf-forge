package com.cfforge.common.dto;

import com.cfforge.common.enums.TriggerType;
import java.util.UUID;

public record BuildRequest(UUID projectId, TriggerType triggerType) {}
