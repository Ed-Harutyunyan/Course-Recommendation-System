package edu.aua.course_recommendation.controller;

import edu.aua.course_recommendation.dto.response.NeededCourseOfferingResponseDto;
import edu.aua.course_recommendation.entity.Schedule;
import edu.aua.course_recommendation.mappers.CourseMapper;
import edu.aua.course_recommendation.service.schedule.NextSemesterScheduleService;
import edu.aua.course_recommendation.service.schedule.ScheduleService;
import edu.aua.course_recommendation.util.AcademicCalendarUtil;
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
    private final CourseMapper courseMapper;

    @GetMapping("/generate")
    public ResponseEntity<Schedule> generateNextSemesterSchedule(
            @RequestParam UUID studentId) {
        return ResponseEntity.ok(nextSemesterScheduleService.generateNextSemester(studentId));
    }

    @GetMapping("/generate/custom")
    public ResponseEntity<Schedule> generateNextSemesterSchedule(
            @RequestParam UUID studentId,
            @RequestParam String year,
            @RequestParam String semester) {
        return ResponseEntity.ok(nextSemesterScheduleService.generateNextSemesterCustom(studentId, year, semester));
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

    @GetMapping("/recommendation/{studentId}/custom/{year}/{semester}")
    public ResponseEntity<List<NeededCourseOfferingResponseDto>> getRecommendedSchedulesForPeriod(
            @PathVariable UUID studentId,
            @PathVariable String year,
            @PathVariable String semester) {

        var neededDtos = scheduleService.findValidOfferingsForPeriod(studentId, year, semester);
        var responseDtos = neededDtos.stream()
                .map(dto -> new NeededCourseOfferingResponseDto(
                        dto.getRequirement(),
                        courseMapper.toCourseOfferingResponseDto(dto.getCourseOffering())
                ))
                .toList();

        return ResponseEntity.ok(responseDtos);
    }

    @GetMapping(value = "/recommendation/{studentId}/custom/{year}/{semester}", params = "message")
    public ResponseEntity<List<NeededCourseOfferingResponseDto>> getRecommendedSchedulesForPeriod(
            @PathVariable UUID studentId,
            @PathVariable String year,
            @PathVariable String semester,
            @RequestParam(required = false) String message) {

        var neededDtos = scheduleService.findValidOfferingsForPeriodWithMessage(studentId, year, semester, message);
        var responseDtos = neededDtos.stream()
                .map(dto -> new NeededCourseOfferingResponseDto(
                        dto.getRequirement(),
                        courseMapper.toCourseOfferingResponseDto(dto.getCourseOffering())
                ))
                .toList();

        return ResponseEntity.ok(responseDtos);
    }


    @GetMapping("/recommendation/{studentId}")
    public ResponseEntity<List<NeededCourseOfferingResponseDto>> getRecommendedSchedules(@PathVariable UUID studentId) {
        String[] nextPeriod = AcademicCalendarUtil.getNextAcademicPeriod();
        String year = nextPeriod[0];
        String semester = nextPeriod[1];

        var neededDtos = scheduleService.findValidOfferingsForPeriod(studentId, year, semester);
        var responseDtos = neededDtos.stream()
                .map(dto -> new NeededCourseOfferingResponseDto(
                        dto.getRequirement(),
                        courseMapper.toCourseOfferingResponseDto(dto.getCourseOffering())
                ))
                .toList();

        return ResponseEntity.ok(responseDtos);
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

    @PutMapping("/update/{id}")
    public ResponseEntity<Schedule> updateSchedule(@PathVariable UUID id, @RequestBody Schedule schedule) {
        scheduleService.getScheduleById(id);
        schedule.setId(id);
        Schedule updatedSchedule = scheduleService.saveSchedule(schedule);
        return ResponseEntity.ok(updatedSchedule);
    }

    @DeleteMapping("/delete/{id}")
    public ResponseEntity<Void> deleteSchedule(@PathVariable UUID id) {
        Schedule schedule = scheduleService.getScheduleById(id);
        scheduleService.deleteSchedule(schedule.getId());
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }

}
