package com.plandosee.diary.export.web;

import java.nio.charset.StandardCharsets;
import java.util.Map;

import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

import com.plandosee.diary.export.application.ExportService;

/**
 * JSON export. The download is a single UTF-8 JSON attachment; GET never changes state.
 */
@Controller
public class ExportController {

    private final ExportService exportService;

    public ExportController(ExportService exportService) {
        this.exportService = exportService;
    }

    @GetMapping("/export")
    public String index() {
        return "export/index";
    }

    @GetMapping("/export/download")
    public ResponseEntity<Map<String, Object>> download() {
        Map<String, Object> document = exportService.export();
        return ResponseEntity.ok()
                .contentType(new MediaType(MediaType.APPLICATION_JSON, StandardCharsets.UTF_8))
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment().filename(exportService.fileName()).build().toString())
                .header(HttpHeaders.CACHE_CONTROL, "no-store")
                .body(document);
    }
}
