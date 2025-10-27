package com.mss301.mindmapservice.service;

import java.util.List;

import com.mss301.mindmapservice.dto.request.FormulaRequest;
import com.mss301.mindmapservice.dto.response.FormulaResponse;

public interface FormulaService {

    /**
     * Create a new formula for a node
     */
    FormulaResponse createFormula(FormulaRequest request);

    /**
     * Update an existing formula
     */
    FormulaResponse updateFormula(Long formulaId, FormulaRequest request);

    /**
     * Delete a formula
     */
    void deleteFormula(Long formulaId);

    /**
     * Get all formulas for a node
     */
    List<FormulaResponse> getFormulasByNode(Long nodeId);

    /**
     * Get primary formulas for a node
     */
    List<FormulaResponse> getPrimaryFormulasByNode(Long nodeId);

    /**
     * Get a single formula by ID
     */
    FormulaResponse getFormulaById(Long formulaId);
}
