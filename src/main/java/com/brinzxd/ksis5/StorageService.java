package com.brinzxd.ksis5;

import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.*;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
public class StorageService {

    private final Path root = Paths.get("storage").toAbsolutePath().normalize();

    public StorageService() throws IOException {
        Files.createDirectories(root);
    }

    public Path resolve(String uri) {
        String decoded = URLDecoder.decode(uri, StandardCharsets.UTF_8);
        String rel = decoded.startsWith("/") ? decoded.substring(1) : decoded;
        Path resolved = root.resolve(rel).normalize();
        if (!resolved.startsWith(root))
            throw new IllegalArgumentException("Invalid path");
        return resolved;
    }

    public List<String> listDirectory(Path dir) throws IOException {
        try (Stream<Path> entries = Files.list(dir)) {
            return entries.map(p -> p.getFileName().toString())
                    .sorted()
                    .collect(Collectors.toList());
        }
    }

    public ResponseEntity<Resource> buildFileResponse(Path path) throws IOException {
        Resource resource = new UrlResource(path.toUri());
        String contentType = Files.probeContentType(path);
        if (contentType == null) contentType = MediaType.APPLICATION_OCTET_STREAM_VALUE;

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "inline; filename=\"" + path.getFileName() + "\"")
                .contentType(MediaType.parseMediaType(contentType))
                .contentLength(Files.size(path))
                .lastModified(Files.getLastModifiedTime(path).toMillis())
                .body(resource);
    }

    public ResponseEntity<Void> store(Path target, byte[] data) throws IOException {
        if (data == null)
            return ResponseEntity.badRequest().build();
        boolean existed = Files.exists(target);
        Files.createDirectories(target.getParent());
        Files.write(target, data,
                StandardOpenOption.CREATE,
                StandardOpenOption.TRUNCATE_EXISTING,
                StandardOpenOption.WRITE);
        return ResponseEntity.status(existed ? HttpStatus.OK : HttpStatus.CREATED).build();
    }

    public ResponseEntity<Void> copy(Path source, Path target) throws IOException {
        if (Files.notExists(source) || Files.isDirectory(source))
            return ResponseEntity.notFound().build();
        boolean existed = Files.exists(target);
        Files.createDirectories(target.getParent());
        Files.copy(source, target, StandardCopyOption.REPLACE_EXISTING);
        return ResponseEntity.status(existed ? HttpStatus.OK : HttpStatus.CREATED).build();
    }
}