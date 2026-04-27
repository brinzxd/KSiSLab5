package com.brinzxd.ksis5;


import jakarta.servlet.http.HttpServletRequest;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.*;
import org.springframework.stereotype.Controller;
import org.springframework.util.FileSystemUtils;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Controller
@RequestMapping("/**")
public class StorageController {

    private final StorageService storageService;

    public StorageController(StorageService storageService) {
        this.storageService = storageService;
    }

    @GetMapping
    public ResponseEntity<?> get(HttpServletRequest request) throws IOException {
        Path path = storageService.resolve(request.getRequestURI());

        if (Files.notExists(path))
            return ResponseEntity.notFound().build();

        if (Files.isDirectory(path))
            return ResponseEntity.ok(storageService.listDirectory(path));

        return storageService.buildFileResponse(path);
    }

    @PutMapping
    public ResponseEntity<?> put(
            @RequestHeader(value = "X-Copy-From", required = false) String copyFrom,
            @RequestBody(required = false) byte[] body,
            HttpServletRequest request
    ) throws IOException {
        Path target = storageService.resolve(request.getRequestURI());

        if (copyFrom != null && !copyFrom.isBlank())
            return storageService.copy(storageService.resolve(copyFrom), target);

        return storageService.store(target, body);
    }

    @RequestMapping(method = RequestMethod.HEAD)
    public ResponseEntity<?> head(HttpServletRequest request) throws IOException {
        Path path = storageService.resolve(request.getRequestURI());

        if (Files.notExists(path))
            return ResponseEntity.notFound().build();

        return ResponseEntity.ok()
                .contentLength(Files.isDirectory(path) ? 0 : Files.size(path))
                .lastModified(Files.getLastModifiedTime(path).toMillis())
                .build();
    }

    @DeleteMapping
    public ResponseEntity<?> delete(HttpServletRequest request) throws IOException {
        Path path = storageService.resolve(request.getRequestURI());

        if (Files.notExists(path))
            return ResponseEntity.notFound().build();

        if (Files.isDirectory(path))
            FileSystemUtils.deleteRecursively(path);
        else
            Files.delete(path);

        return ResponseEntity.noContent().build();
    }
}