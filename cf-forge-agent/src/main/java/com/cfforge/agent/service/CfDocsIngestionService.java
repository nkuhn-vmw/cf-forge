package com.cfforge.agent.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.reader.TextReader;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.core.io.FileSystemResource;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

@Service
@Slf4j
public class CfDocsIngestionService {

    private final VectorStore vectorStore;

    public CfDocsIngestionService(VectorStore vectorStore) {
        this.vectorStore = vectorStore;
    }

    public int ingestDirectory(Path docsDirectory) throws IOException {
        var splitter = new TokenTextSplitter(800, 200, 5, 10000, true);
        List<Document> allDocs = new ArrayList<>();

        try (Stream<Path> files = Files.walk(docsDirectory)) {
            files.filter(p -> p.toString().endsWith(".md") || p.toString().endsWith(".txt") || p.toString().endsWith(".adoc"))
                .forEach(file -> {
                    try {
                        var reader = new TextReader(new FileSystemResource(file));
                        reader.getCustomMetadata().put("source", file.getFileName().toString());
                        reader.getCustomMetadata().put("path", docsDirectory.relativize(file).toString());
                        List<Document> docs = splitter.apply(reader.get());
                        allDocs.addAll(docs);
                    } catch (Exception e) {
                        log.warn("Failed to ingest file {}: {}", file, e.getMessage());
                    }
                });
        }

        if (!allDocs.isEmpty()) {
            vectorStore.add(allDocs);
            log.info("Ingested {} document chunks from {}", allDocs.size(), docsDirectory);
        }
        return allDocs.size();
    }

    public int ingestText(String content, String source) {
        var splitter = new TokenTextSplitter(800, 200, 5, 10000, true);
        var doc = new Document(content, Map.of("source", source));
        List<Document> chunks = splitter.apply(List.of(doc));
        vectorStore.add(chunks);
        return chunks.size();
    }
}
