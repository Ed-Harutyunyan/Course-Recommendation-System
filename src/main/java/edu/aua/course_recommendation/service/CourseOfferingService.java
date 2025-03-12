package edu.aua.course_recommendation.service;

import edu.aua.course_recommendation.dto.CourseOfferingDto;
import edu.aua.course_recommendation.entity.CourseOffering;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CourseOfferingService {

    private final CourseService courseService;

    public CourseOffering createCourseOffering(CourseOfferingDto courseOfferingDto) {
        return null;
    }


}
