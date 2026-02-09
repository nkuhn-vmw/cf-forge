package com.cfforge.api.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record CfPaginatedResponse<T>(Pagination pagination, List<T> resources) {
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Pagination(int total_results, int total_pages) {}
}
