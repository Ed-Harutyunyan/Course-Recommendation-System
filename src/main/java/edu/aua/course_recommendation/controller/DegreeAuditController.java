package edu.aua.course_recommendation.controller;

import edu.aua.course_recommendation.entity.User;
import edu.aua.course_recommendation.model.DegreeAuditMultiScenarioResult;
import edu.aua.course_recommendation.model.Department;
import edu.aua.course_recommendation.service.audit.BaseDegreeAuditService;
import edu.aua.course_recommendation.service.audit.CSDegreeAuditService;
import edu.aua.course_recommendation.service.auth.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/degree-audit")
public class DegreeAuditController {

    private final Map<Department, BaseDegreeAuditService> deptServices;
    private final UserService userService;

    public DegreeAuditController(
            CSDegreeAuditService csService, UserService userService) {
        Map<Department, BaseDegreeAuditService> map = new HashMap<>();
        map.put(Department.CS, csService);
        // map.put(Department.BUSINESS, businessService);
        // ...
        this.deptServices = Collections.unmodifiableMap(map);
        this.userService = userService;
    }

    @GetMapping("/{studentId}")
    public ResponseEntity<DegreeAuditMultiScenarioResult> auditDegree(
            @PathVariable UUID studentId
    ) {
        User student = userService.findById(studentId);
        BaseDegreeAuditService service = deptServices.get(student.getDepartment());
        if (service == null) {
            return ResponseEntity.badRequest().body(null);
        }
        return ResponseEntity.ok(service.auditStudentDegreeMultiScenario(studentId));
    }

    @GetMapping("/gened/{studentId}")
    public ResponseEntity<?> getGenedOptions(@PathVariable UUID studentId) {
        User student = userService.findById(studentId);
        BaseDegreeAuditService service = deptServices.get(student.getDepartment());
        if (service == null) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "No audit service for department: " + student.getDepartment()));
        }
        return ResponseEntity.ok(service.checkGeneralEducationRequirementsDetailed(studentId));
    }

    @GetMapping("/gened/missing/{studentId}")
    public ResponseEntity<?> getGenedMissing(@PathVariable UUID studentId) {
        User student = userService.findById(studentId);
        BaseDegreeAuditService service = deptServices.get(student.getDepartment());
        if (service == null) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "No audit service for department: " + student.getDepartment()));
        }
        return ResponseEntity.ok(service.getNeededClusters(studentId));
    }

    @GetMapping("/gened/clusters/{studentId}")
    public ResponseEntity<?> getGenedClusters(@PathVariable UUID studentId) {
        User student = userService.findById(studentId);
        BaseDegreeAuditService service = deptServices.get(student.getDepartment());
        if (service == null) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "No audit service for department: " + student.getDepartment()));
        }
        return ResponseEntity.ok(service.getClusters(studentId));
    }

}
