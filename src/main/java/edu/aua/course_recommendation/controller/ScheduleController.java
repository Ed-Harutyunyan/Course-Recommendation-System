package edu.aua.course_recommendation.controller;

import edu.aua.course_recommendation.dto.CourseOfferingResponseDto;
import edu.aua.course_recommendation.entity.CourseOffering;
import edu.aua.course_recommendation.entity.Schedule;
import edu.aua.course_recommendation.mappers.CourseMapper;
import edu.aua.course_recommendation.service.NextSemesterScheduleService;
import edu.aua.course_recommendation.service.ScheduleService;
import edu.aua.course_recommendation.service.course.CourseOfferingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/schedule")
public class ScheduleController {

    private final NextSemesterScheduleService nextSemesterScheduleService;
    private final ScheduleService scheduleService;
    private final CourseOfferingService courseOfferingService;
    private final CourseMapper courseMapper;

    @GetMapping("/generate")
    public ResponseEntity<Schedule> generateNextSemesterSchedule(
            @RequestParam UUID studentId,
            @RequestParam String year,
            @RequestParam String semester) {
        return ResponseEntity.ok(nextSemesterScheduleService.generateNextSemester(studentId, year, semester));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Schedule> getSchedule(@PathVariable UUID id) {
        Schedule schedule = scheduleService.getScheduleById(id);
        if (schedule != null) {
            return ResponseEntity.ok(schedule);
        } else {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
    }

    @GetMapping("/recommendation/{studentId}")
    public ResponseEntity<List<CourseOfferingResponseDto>> getRecommendedSchedules(@PathVariable UUID studentId) {

        List<CourseOffering> offerings = scheduleService.findValidOfferings(studentId);

        List<CourseOfferingResponseDto> offeringDtos = offerings.stream()
                .map(courseMapper::toCourseOfferingResponseDto)
                .toList();

        return ResponseEntity.ok(offeringDtos);
    }

    @GetMapping("/all/{studentId}")
    public ResponseEntity<List<Schedule>> getSchedulesByStudentId(@PathVariable UUID studentId) {
        List<Schedule> schedules = scheduleService.getSchedulesByStudentId(studentId);
        return ResponseEntity.ok(schedules);
    }


    @GetMapping("/all")
    public ResponseEntity<List<Schedule>> getAllSchedules() {
        List<Schedule> schedules = scheduleService.getAllSchedules();
        return ResponseEntity.ok(schedules);
    }

    @PostMapping("/save")
    public ResponseEntity<Schedule> saveSchedule(@RequestBody Schedule schedule) {
        Schedule saved = scheduleService.saveSchedule(schedule);
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteSchedule(@PathVariable UUID id) {
        Schedule schedule = scheduleService.getScheduleById(id);
        if (schedule != null) {
            scheduleService.deleteSchedule(id);
            return ResponseEntity.noContent().build();
        } else {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
    }

}
