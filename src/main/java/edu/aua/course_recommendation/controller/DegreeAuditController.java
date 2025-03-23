package edu.aua.course_recommendation.controller;

import edu.aua.course_recommendation.model.DegreeAuditMultiScenarioResult;
import edu.aua.course_recommendation.model.Department;
import edu.aua.course_recommendation.model.NeededCluster;
import edu.aua.course_recommendation.repository.UserRepository;
import edu.aua.course_recommendation.service.audit.BaseDegreeAuditService;
import edu.aua.course_recommendation.service.audit.CSDegreeAuditService;
import edu.aua.course_recommendation.service.audit.GenedClusteringService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/degree-audit")
public class DegreeAuditController {

    private final Map<Department, BaseDegreeAuditService> deptServices;

    public DegreeAuditController(
            CSDegreeAuditService csService,
            UserRepository userRepository) {
        Map<Department, BaseDegreeAuditService> map = new HashMap<>();
        map.put(Department.CS, csService);
        // map.put(Department.BUSINESS, businessService);
        // ...
        this.deptServices = Collections.unmodifiableMap(map);
    }

    @GetMapping("/{department}/{studentId}")
    public ResponseEntity<DegreeAuditMultiScenarioResult> auditDegree(
            @PathVariable("department") Department department,
            @PathVariable UUID studentId
    ) {
        BaseDegreeAuditService service = deptServices.get(department);
        if (service == null) {
            return ResponseEntity.badRequest().body(null);
        }
        // produce multi-scenario result
        DegreeAuditMultiScenarioResult result = service.auditStudentDegreeMultiScenario(studentId);
        return ResponseEntity.ok(result);
    }

    @GetMapping("{department}/gened/check/{studentId}")
    public ResponseEntity<?> checkGenedDegree(
            @PathVariable("department") Department department,
            @PathVariable UUID studentId
    ) {
        // 1. Fetch the appropriate BaseDegreeAuditService for this department
        BaseDegreeAuditService service = deptServices.get(department);
        if (service == null) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "No audit service for department: " + department));
        }

        boolean result = service.checkGened(studentId);
        return ResponseEntity.ok(result);
    }

    @GetMapping("{department}/gened/{studentId}")
    public ResponseEntity<?> getGenedOptions(
            @PathVariable("department") Department department,
            @PathVariable UUID studentId
    ) {
        // 1. Fetch the appropriate BaseDegreeAuditService for this department
        BaseDegreeAuditService service = deptServices.get(department);
        if (service == null) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "No audit service for department: " + department));
        }
        List<GenedClusteringService.ClusterSolution> result = service.checkGeneralEducationRequirements(studentId);
        return ResponseEntity.ok(result);
    }

    @GetMapping("{department}/gened/missing/{studentId}")
    public ResponseEntity<?> getGenedMissing(
            @PathVariable("department") Department department,
            @PathVariable UUID studentId
    ) {
        // 1. Fetch the appropriate BaseDegreeAuditService for this department
        BaseDegreeAuditService service = deptServices.get(department);
        if (service == null) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "No audit service for department: " + department));
        }
        Set<NeededCluster> result = service.getGenedMissing(studentId);
        return ResponseEntity.ok(result);
    }

}
