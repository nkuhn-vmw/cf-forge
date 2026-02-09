package com.cfforge.builder.security;

import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
@Builder
public class CveScanResult {
    private boolean scanned;
    private List<CveScanner.Vulnerability> vulnerabilities;
    private Map<String, Integer> severityCounts;
    private boolean blocked;
    private String summary;
    private int exitCode;
}
