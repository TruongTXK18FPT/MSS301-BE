package com.mss301.mindmapservice.controller;

import java.util.List;

import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.mss301.mindmapservice.dto.ApiResponse;
import com.mss301.mindmapservice.dto.request.FormulaRequest;
import com.mss301.mindmapservice.dto.response.FormulaResponse;
import com.mss301.mindmapservice.service.FormulaService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/mindmap/formulas")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Formula Management", description = "APIs for managing formulas in mindmap nodes")
public class FormulaController {

    private final FormulaService formulaService;

    @PostMapping
    @Operation(summary = "Create a new formula", description = "Create a new formula for a mindmap node")
    public ResponseEntity<ApiResponse<FormulaResponse>> createFormula(
            @Valid @RequestBody FormulaRequest request) {
        
        FormulaResponse response = formulaService.createFormula(request);
        
        return ResponseEntity.ok(ApiResponse.<FormulaResponse>builder()
                .code("200")
                .message("Formula created successfully")
                .result(response)
                .build());
    }

    @PutMapping("/{formulaId}")
    @Operation(summary = "Update a formula", description = "Update an existing formula")
    public ResponseEntity<ApiResponse<FormulaResponse>> updateFormula(
            @PathVariable Long formulaId,
            @Valid @RequestBody FormulaRequest request) {
        
        FormulaResponse response = formulaService.updateFormula(formulaId, request);
        
        return ResponseEntity.ok(ApiResponse.<FormulaResponse>builder()
                .code("200")
                .message("Formula updated successfully")
                .result(response)
                .build());
    }

    @DeleteMapping("/{formulaId}")
    @Operation(summary = "Delete a formula", description = "Delete a formula by ID")
    public ResponseEntity<ApiResponse<Void>> deleteFormula(@PathVariable Long formulaId) {
        
        formulaService.deleteFormula(formulaId);
        
        return ResponseEntity.ok(ApiResponse.<Void>builder()
                .code("200")
                .message("Formula deleted successfully")
                .build());
    }

    @GetMapping("/node/{nodeId}")
    @Operation(summary = "Get formulas by node", description = "Get all formulas for a specific node")
    public ResponseEntity<ApiResponse<List<FormulaResponse>>> getFormulasByNode(
            @PathVariable Long nodeId) {
        
        List<FormulaResponse> responses = formulaService.getFormulasByNode(nodeId);
        
        return ResponseEntity.ok(ApiResponse.<List<FormulaResponse>>builder()
                .code("200")
                .message("Formulas retrieved successfully")
                .result(responses)
                .build());
    }

    @GetMapping("/node/{nodeId}/primary")
    @Operation(summary = "Get primary formulas", description = "Get primary formulas for a node")
    public ResponseEntity<ApiResponse<List<FormulaResponse>>> getPrimaryFormulas(
            @PathVariable Long nodeId) {
        
        List<FormulaResponse> responses = formulaService.getPrimaryFormulasByNode(nodeId);
        
        return ResponseEntity.ok(ApiResponse.<List<FormulaResponse>>builder()
                .code("200")
                .message("Primary formulas retrieved successfully")
                .result(responses)
                .build());
    }

    @GetMapping("/{formulaId}")
    @Operation(summary = "Get formula by ID", description = "Get a specific formula by its ID")
    public ResponseEntity<ApiResponse<FormulaResponse>> getFormulaById(
            @PathVariable Long formulaId) {
        
        FormulaResponse response = formulaService.getFormulaById(formulaId);
        
        return ResponseEntity.ok(ApiResponse.<FormulaResponse>builder()
                .code("200")
                .message("Formula retrieved successfully")
                .result(response)
                .build());
    }
}
