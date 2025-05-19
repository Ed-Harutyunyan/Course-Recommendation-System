package edu.aua.course_recommendation.service.schedule;

import edu.aua.course_recommendation.dto.request.MessageAndPossibleCourseDto;
import edu.aua.course_recommendation.dto.request.PassedAndPossibleCoursesDto;
import edu.aua.course_recommendation.dto.response.CourseDto;
import edu.aua.course_recommendation.dto.response.CourseResponseDto;
import edu.aua.course_recommendation.dto.response.RecommendationDto;
import edu.aua.course_recommendation.exceptions.RecommendationException;
import edu.aua.course_recommendation.mappers.CourseMapper;
import edu.aua.course_recommendation.service.course.CourseService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PythonService {

    private final RestTemplate restTemplate = new RestTemplate();
    private final CourseMapper courseMapper;
    private final CourseService courseService;
    @Value("${python.service.url}")
    private String pythonServiceEndpoint;

    public String sendCourses() {
        HttpHeaders headers = new HttpHeaders();

        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<List<CourseResponseDto>> request =
                new HttpEntity<>(courseService
                        .getAllCourses()
                        .stream()
                        .map(courseMapper::toCourseResponseDto)
                        .toList(), headers);

        ResponseEntity<String> response = restTemplate.exchange(pythonServiceEndpoint + "/api/vectorize",
                HttpMethod.PUT, request, String.class);

        return response.getBody();
    }

    public List<RecommendationDto> sendMessageRecommendations(MessageAndPossibleCourseDto body) {
        HttpHeaders headers = new HttpHeaders();
        String URL = pythonServiceEndpoint + "/api/recommend/message";

        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<MessageAndPossibleCourseDto> request = new HttpEntity<>(body, headers);

        ResponseEntity<List<RecommendationDto>> response = restTemplate.exchange(
                URL, HttpMethod.POST, request, new ParameterizedTypeReference<>() {
                }
        );

        if (response.getStatusCode() != HttpStatus.OK || response.getBody() == null) {
            throw new RecommendationException("Failed to fetch recommendations from Python with status code: "
                    + response.getStatusCode());
        }
        return response.getBody();
    }

    public List<RecommendationDto> getRecommendationsWithPassedCourses(List<String> passedCourseCodes, List<String> possibleCourseCodes) {
        PassedAndPossibleCoursesDto courseCodes = PassedAndPossibleCoursesDto.builder()
                                                                       .passed_course_codes(passedCourseCodes)
                                                                       .possible_course_codes(possibleCourseCodes)
                                                                       .build();

        HttpHeaders headers = new HttpHeaders();
        String URL = pythonServiceEndpoint + "/api/recommend/byPassed";

        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<PassedAndPossibleCoursesDto> request = new HttpEntity<>(courseCodes, headers);

        ResponseEntity<List<RecommendationDto>> response = restTemplate.exchange(
                URL, HttpMethod.POST, request, new ParameterizedTypeReference<>() {
                }
        );

        if (response.getStatusCode() != HttpStatus.OK && response.getBody() == null) {
            throw new RecommendationException("Failed to fetch recommendations from Python with status code: "
                    + response.getStatusCode());
        }
        return response.getBody();
    }

    public ResponseEntity<String> deletePoints() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        String URL = pythonServiceEndpoint + "/api/delete/points";
        HttpEntity<String> request = new HttpEntity<>(headers);

        try {
            ResponseEntity<String> response = restTemplate.exchange(URL, HttpMethod.DELETE, request, String.class);
            if (response.getStatusCode() != HttpStatus.OK || response.getBody() == null) {
                throw new RecommendationException("Failed to delete courses from Python with status code: "
                        + response.getStatusCode());
            }
            return response;
        } catch (Exception e) {
            throw new RecommendationException("An error occurred while attempting to delete courses: " + e.getMessage(), e);
        }
    }

    public String sendTest(List<CourseDto> data) {
        HttpHeaders headers = new HttpHeaders();

        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<List<CourseDto>> request = new HttpEntity<>(data, headers);

        ResponseEntity<String> response = restTemplate.exchange(pythonServiceEndpoint + "api/test",
                HttpMethod.POST, request, String.class);

        return response.getBody();
    }
}
