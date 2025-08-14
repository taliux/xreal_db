package com.xreal.db.controller;

import com.xreal.db.dto.ExcelUploadResponse;
import com.xreal.db.service.ExcelUploadService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/excel")
@RequiredArgsConstructor
@Tag(name = "Excel Upload", description = "Excel文件上传管理接口")
public class ExcelUploadController {

    private final ExcelUploadService excelUploadService;

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(
        summary = "Upload Excel file",
        description = "Upload Excel file containing xreal_tech_faq and tag sheets to update database",
        responses = {
            @ApiResponse(
                responseCode = "200",
                description = "Upload successful",
                content = @Content(schema = @Schema(implementation = ExcelUploadResponse.class))
            ),
            @ApiResponse(
                responseCode = "400",
                description = "Invalid file format or data validation failed",
                content = @Content(schema = @Schema(implementation = String.class))
            )
        }
    )
    public ResponseEntity<ExcelUploadResponse> uploadExcel(
            @RequestParam("file") MultipartFile file) {
        
        if (file.isEmpty()) {
            throw new IllegalArgumentException("File cannot be empty");
        }
        
        String filename = file.getOriginalFilename();
        if (filename == null || (!filename.endsWith(".xlsx") && !filename.endsWith(".xls"))) {
            throw new IllegalArgumentException("File must be Excel format (.xlsx or .xls)");
        }
        
        try {
            ExcelUploadResponse response = excelUploadService.processExcelFile(file);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            throw new RuntimeException("Failed to process Excel file: " + e.getMessage(), e);
        }
    }
}