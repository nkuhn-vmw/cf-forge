package com.cfforge.common.dto;

public record FileEntry(
    String name,
    String path,
    boolean directory,
    long size,
    String lastModified
) {}
