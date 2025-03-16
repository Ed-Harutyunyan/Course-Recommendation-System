package edu.aua.course_recommendation.controller;

import edu.aua.course_recommendation.dto.CourseDto;
import edu.aua.course_recommendation.service.PythonService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("api/python")
@RequiredArgsConstructor
public class PythonController {

    private final PythonService pythonService;

//    Testing only
//    @PostMapping("/send")
//    public String sendPython(@RequestBody Map<String, Object> data) {
//        return pythonService.sendTest(data);
//    }

    @PostMapping("/newCourses")
    public String newCourses(@RequestBody List<CourseDto> data) {
        return pythonService.sendCourse(data);
    }

}
