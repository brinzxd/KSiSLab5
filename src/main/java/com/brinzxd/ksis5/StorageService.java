package com.brinzxd.ksis5;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.FileSystemUtils;

import java.io.IOException;
import java.io.InputStream;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
public class StorageService {

    private final Path root;

    public StorageService(@Value("${storage.root:./storage}") String storageRoot) throws IOException {
        this.root = Paths.get(storageRoot).toAbsolutePath().normalize();
        Files.createDirectories(this.root);
    }

    public Path resolve(String uri) {
        String decoded = URLDecoder.decode(uri, StandardCharsets.UTF_8);
        String rel = decoded.startsWith("/") ? decoded.substring(1) : decoded;
        Path resolved = root.resolve(rel).normalize();
        if (!resolved.startsWith(root))
            throw new SecurityException("Path traversal detected");
        return resolved;
    }

    // PUT — сохранить файл из потока
    public ResponseEntity<Void> store(Path target, InputStream data) throws IOException {
        boolean existed = Files.exists(target);
        if (target.getParent() != null)
            Files.createDirectories(target.getParent());
        Files.copy(data, target, StandardCopyOption.REPLACE_EXISTING);
        return ResponseEntity
                .status(existed ? HttpStatus.OK : HttpStatus.CREATED)
                .build();
    }

    // PUT + X-Copy-From — скопировать файл
    public ResponseEntity<Void> copy(Path source, Path dest) throws IOException {
        if (!Files.exists(source))
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        if (Files.isDirectory(source))
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        boolean existed = Files.exists(dest);
        if (dest.getParent() != null)
            Files.createDirectories(dest.getParent());
        Files.copy(source, dest, StandardCopyOption.REPLACE_EXISTING);
        return ResponseEntity
                .status(existed ? HttpStatus.OK : HttpStatus.CREATED)
                .build();
    }

    // GET — скачать файл
    public ResponseEntity<Resource> download(Path path) throws IOException {
        Resource resource = new UrlResource(path.toUri());
        String contentType = Files.probeContentType(path);
        if (contentType == null)
            contentType = MediaType.APPLICATION_OCTET_STREAM_VALUE;
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .contentLength(Files.size(path))
                .header(HttpHeaders.LAST_MODIFIED,
                        String.valueOf(Files.getLastModifiedTime(path).toMillis()))
                .body(resource);
    }

    // GET — список каталога
    public ResponseEntity<List<String>> listDir(Path dir) throws IOException {
        try (Stream<Path> stream = Files.list(dir)) {
            List<String> names = stream
                    .map(p -> p.getFileName().toString()
                            + (Files.isDirectory(p) ? "/" : ""))
                    .sorted()
                    .collect(Collectors.toList());
            return ResponseEntity.ok(names);
        }
    }

    // HEAD — метаданные файла
    public ResponseEntity<Void> metadata(Path path) throws IOException {
        return ResponseEntity.ok()
                .contentLength(Files.size(path))
                .header(HttpHeaders.CONTENT_TYPE,
                        Files.probeContentType(path) != null
                                ? Files.probeContentType(path)
                                : MediaType.APPLICATION_OCTET_STREAM_VALUE)
                .header(HttpHeaders.LAST_MODIFIED,
                        String.valueOf(Files.getLastModifiedTime(path).toMillis()))
                .build();
    }

    // DELETE — удалить файл или каталог
    public ResponseEntity<Void> delete(Path path) throws IOException {
        if (Files.isDirectory(path))
            FileSystemUtils.deleteRecursively(path);
        else
            Files.delete(path);
        return ResponseEntity.noContent().build();
    }

    public boolean exists(Path p) { return Files.exists(p); }
    public boolean isDir(Path p)  { return Files.isDirectory(p); }
}