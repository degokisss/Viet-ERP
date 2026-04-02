package com.vieterp.otb.dataretention;

import com.vieterp.otb.dataretention.dto.CleanupResult;
import com.vieterp.otb.dataretention.dto.RetentionPolicyResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/data-retention")
@RequiredArgsConstructor
@Tag(name = "Data Retention", description = "Data retention policy and cleanup management")
public class DataRetentionController {

    private final DataRetentionService dataRetentionService;

    @GetMapping("/policy")
    @Operation(summary = "Get current data retention policy")
    public ResponseEntity<RetentionPolicyResponse> getRetentionPolicy() {
        return ResponseEntity.ok(dataRetentionService.getRetentionPolicy());
    }

    @PostMapping("/cleanup")
    @Operation(summary = "Execute data retention cleanup")
    public ResponseEntity<CleanupResult> cleanup() {
        return ResponseEntity.ok(dataRetentionService.cleanup());
    }
}
