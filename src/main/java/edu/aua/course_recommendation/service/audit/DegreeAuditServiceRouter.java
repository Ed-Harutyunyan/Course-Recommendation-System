package edu.aua.course_recommendation.service.audit;

import edu.aua.course_recommendation.model.Department;
import edu.aua.course_recommendation.service.auth.UserService;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class DegreeAuditServiceRouter {

    private final Map<Department, BaseDegreeAuditService> serviceMap;
    private final UserService userService;

    public DegreeAuditServiceRouter(List<BaseDegreeAuditService> auditServices, UserService userService) {
        this.userService = userService;

        this.serviceMap = Map.of(
                Department.BAB, findServiceByType(auditServices, BusinessDegreeAuditService.class),
                Department.CS, findServiceByType(auditServices, CSDegreeAuditService.class),
                Department.DS, findServiceByType(auditServices, CSDegreeAuditService.class)
        );
    }

    public BaseDegreeAuditService getServiceForStudent(UUID studentId) {
        Department department = userService.getStudentDepartment(studentId);
        BaseDegreeAuditService service = serviceMap.get(department);

        if (service == null) {
            throw new IllegalStateException("No degree audit service found for department: " + department);
        }

        return service;
    }

    private <T extends BaseDegreeAuditService> T findServiceByType(List<BaseDegreeAuditService> services, Class<T> type) {
        return services.stream()
                .filter(type::isInstance)
                .map(type::cast)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("No audit service of type " + type.getSimpleName() + " found"));
    }
}