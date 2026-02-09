package com.cfforge.agent.model;

public record GeneratedFile(String path, String content, FileAction action) {
    public enum FileAction { CREATE, UPDATE, DELETE }
}
