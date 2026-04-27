package com.brinzxd.ksis5;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.nio.file.Path;

@RestController
@RequestMapping("/**")
public class StorageController {

    private final StorageService service;

    public StorageController(StorageService service) {
        this.service = service;
    }

    @GetMapping
    public ResponseEntity<?> get(HttpServletRequest request) throws IOException {
        Path path = service.resolve(request.getRequestURI());

        if (!service.exists(path))
            return ResponseEntity.notFound().build();

        if (service.isDir(path))
            return service.listDir(path);

        return service.download(path);
    }

    @PutMapping
    public ResponseEntity<?> put(
            @RequestHeader(value = "X-Copy-From", required = false) String copyFrom,
            HttpServletRequest request
    ) throws IOException {
        Path target = service.resolve(request.getRequestURI());

        if (copyFrom != null && !copyFrom.isBlank())
            return service.copy(service.resolve(copyFrom), target);

        return service.store(target, request.getInputStream());
    }

    @RequestMapping(method = RequestMethod.HEAD)
    public ResponseEntity<?> head(HttpServletRequest request) throws IOException {
        Path path = service.resolve(request.getRequestURI());

        if (!service.exists(path))
            return ResponseEntity.notFound().build();
        if (service.isDir(path))
            return ResponseEntity.badRequest().build();

        return service.metadata(path);
    }

    @DeleteMapping
    public ResponseEntity<?> delete(HttpServletRequest request) throws IOException {
        Path path = service.resolve(request.getRequestURI());

        if (!service.exists(path))
            return ResponseEntity.notFound().build();

        return service.delete(path);
    }
}