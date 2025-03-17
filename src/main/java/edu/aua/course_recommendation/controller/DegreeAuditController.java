package edu.aua.course_recommendation.controller;

import edu.aua.course_recommendation.service.audit.BaseDegreeAuditService;
import edu.aua.course_recommendation.service.audit.CSDegreeAuditService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/degree-audit")
public class DegreeAuditController {

    private final Map<String, BaseDegreeAuditService> auditServices;

    public DegreeAuditController(CSDegreeAuditService csDegreeAuditService) {
        this.auditServices = new HashMap<>();
        this.auditServices.put("cs", csDegreeAuditService);
    }

    @GetMapping("/{program}/{studentId}")
    public ResponseEntity<?> auditDegree(
            @PathVariable String program,
            @PathVariable UUID studentId) {

        BaseDegreeAuditService auditService = auditServices.get(program.toLowerCase());
        if (auditService == null) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Invalid program: " + program));
        }

        List<String> missing = auditService.auditStudentDegree(studentId);
        Map<String, Object> response = new HashMap<>();
        response.put("canGraduate", missing.isEmpty());
        response.put("missingRequirements", missing);

        return ResponseEntity.ok(response);
    }
}