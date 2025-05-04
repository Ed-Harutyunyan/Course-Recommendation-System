package edu.aua.course_recommendation.controller;

import edu.aua.course_recommendation.entity.User;
import edu.aua.course_recommendation.model.DegreeAuditMultiScenarioResult;
import edu.aua.course_recommendation.service.audit.BaseDegreeAuditService;
import edu.aua.course_recommendation.service.audit.DegreeAuditServiceRouter;
import edu.aua.course_recommendation.service.auth.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/degree-audit")
public class DegreeAuditController {

    private final DegreeAuditServiceRouter degreeAuditServiceRouter;
    private final UserService userService;

    public DegreeAuditController(
            DegreeAuditServiceRouter degreeAuditServiceRouter, UserService userService) {
        this.degreeAuditServiceRouter = degreeAuditServiceRouter;
        this.userService = userService;
    }

    @GetMapping("/{studentId}")
    public ResponseEntity<DegreeAuditMultiScenarioResult> auditDegree(
            @PathVariable UUID studentId
    ) {
        try {
            BaseDegreeAuditService service = degreeAuditServiceRouter.getServiceForStudent(studentId);
            return ResponseEntity.ok(service.auditStudentDegreeMultiScenario(studentId));
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().body(null);
        }
    }

    @GetMapping("/gened/{studentId}")
    public ResponseEntity<?> getGenedOptions(@PathVariable UUID studentId) {
        try {
            BaseDegreeAuditService service = degreeAuditServiceRouter.getServiceForStudent(studentId);
            return ResponseEntity.ok(service.checkGeneralEducationRequirementsDetailed(studentId));
        } catch (IllegalStateException e) {
            User student = userService.findById(studentId);
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "No audit service for department: " + student.getDepartment()));
        }
    }

    @GetMapping("/gened/missing/{studentId}")
    public ResponseEntity<?> getGenedMissing(@PathVariable UUID studentId) {
        try {
            BaseDegreeAuditService service = degreeAuditServiceRouter.getServiceForStudent(studentId);
            return ResponseEntity.ok(service.getNeededClusters(studentId));
        } catch (IllegalStateException e) {
            User student = userService.findById(studentId);
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "No audit service for department: " + student.getDepartment()));
        }
    }

    @GetMapping("/gened/clusters/{studentId}")
    public ResponseEntity<?> getGenedClusters(@PathVariable UUID studentId) {
        try {
            BaseDegreeAuditService service = degreeAuditServiceRouter.getServiceForStudent(studentId);
            return ResponseEntity.ok(service.getClusters(studentId));
        } catch (IllegalStateException e) {
            User student = userService.findById(studentId);
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "No audit service for department: " + student.getDepartment()));
        }
    }
}