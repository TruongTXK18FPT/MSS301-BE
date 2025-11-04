package com.mss301.mindmapservice.service.impl;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.mss301.mindmapservice.dto.request.FormulaRequest;
import com.mss301.mindmapservice.dto.response.FormulaResponse;
import com.mss301.mindmapservice.entity.Formula;
import com.mss301.mindmapservice.repository.FormulaRepository;
import com.mss301.mindmapservice.repository.MindmapNodeRepository;
import com.mss301.mindmapservice.service.FormulaService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class FormulaServiceImpl implements FormulaService {

    private final FormulaRepository formulaRepository;
    private final MindmapNodeRepository mindmapNodeRepository;

    @Override
    @Transactional
    public FormulaResponse createFormula(FormulaRequest request) {
        log.info("Creating formula for node: {}", request.getNodeId());

        if (!mindmapNodeRepository.existsById(request.getNodeId())) {
            throw new RuntimeException("Node not found with id: " + request.getNodeId());
        }

        Formula formula = new Formula();
        formula.setNodeId(request.getNodeId());
        formula.setName(request.getName());
        formula.setFormulaText(request.getFormulaText());
        formula.setFormulaLatex(request.getFormulaLatex());
        formula.setDescription(request.getDescription());
        formula.setUsageExample(request.getUsageExample());
        formula.setVariables(request.getVariables());
        formula.setConditions(request.getConditions());
        formula.setOrderIndex(request.getOrderIndex());
        formula.setIsPrimary(request.getIsPrimary());

        Formula saved = formulaRepository.save(formula);
        log.info("Formula created with id: {}", saved.getId());

        return mapToResponse(saved);
    }

    @Override
    @Transactional
    public FormulaResponse updateFormula(Long formulaId, FormulaRequest request) {
        log.info("Updating formula: {}", formulaId);

        Formula formula = formulaRepository.findById(formulaId)
                .orElseThrow(() -> new RuntimeException("Formula not found with id: " + formulaId));

        formula.setName(request.getName());
        formula.setFormulaText(request.getFormulaText());
        formula.setFormulaLatex(request.getFormulaLatex());
        formula.setDescription(request.getDescription());
        formula.setUsageExample(request.getUsageExample());
        formula.setVariables(request.getVariables());
        formula.setConditions(request.getConditions());
        formula.setOrderIndex(request.getOrderIndex());
        formula.setIsPrimary(request.getIsPrimary());

        Formula updated = formulaRepository.save(formula);
        log.info("Formula updated: {}", formulaId);

        return mapToResponse(updated);
    }

    @Override
    @Transactional
    public void deleteFormula(Long formulaId) {
        log.info("Deleting formula: {}", formulaId);

        Formula formula = formulaRepository.findById(formulaId)
                .orElseThrow(() -> new RuntimeException("Formula not found with id: " + formulaId));

        formulaRepository.delete(formula);
        log.info("Formula deleted: {}", formulaId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<FormulaResponse> getFormulasByNode(Long nodeId) {
        log.info("Getting formulas for node: {}", nodeId);
        return formulaRepository.findByNodeIdOrderByOrderIndexAsc(nodeId).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<FormulaResponse> getPrimaryFormulasByNode(Long nodeId) {
        log.info("Getting primary formulas for node: {}", nodeId);
        return formulaRepository.findByNodeIdAndIsPrimaryTrue(nodeId).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public FormulaResponse getFormulaById(Long formulaId) {
        log.info("Getting formula: {}", formulaId);
        Formula formula = formulaRepository.findById(formulaId)
                .orElseThrow(() -> new RuntimeException("Formula not found with id: " + formulaId));
        return mapToResponse(formula);
    }

    private FormulaResponse mapToResponse(Formula formula) {
        return FormulaResponse.builder()
                .id(formula.getId())
                .nodeId(formula.getNodeId())
                .name(formula.getName())
                .formulaText(formula.getFormulaText())
                .formulaLatex(formula.getFormulaLatex())
                .description(formula.getDescription())
                .usageExample(formula.getUsageExample())
                .variables(formula.getVariables())
                .conditions(formula.getConditions())
                .orderIndex(formula.getOrderIndex())
                .isPrimary(formula.getIsPrimary())
                .createdAt(formula.getCreatedAt())
                .updatedAt(formula.getUpdatedAt())
                .build();
    }
}
