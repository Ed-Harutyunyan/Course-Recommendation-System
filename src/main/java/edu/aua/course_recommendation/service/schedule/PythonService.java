package edu.aua.course_recommendation.service.schedule;

import com.fasterxml.jackson.databind.ObjectMapper;
import edu.aua.course_recommendation.dto.*;
import edu.aua.course_recommendation.mappers.CourseMapper;
import edu.aua.course_recommendation.service.course.CourseService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.io.File;
import java.io.IOException;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PythonService {

    private final RestTemplate restTemplate = new RestTemplate();
    private final CourseMapper courseMapper;

    @Value("${python.service.url}")
    private String pythonServiceEndpoint;
    @Value("${python.sent.recommendations}")
    private String dataPath;

    private final CourseService courseService;

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

    /** For freshman: keywords
     *
     * @param keywords List of keywords that interest the student
     */
    public ResponseEntity<String> sendKeywordsRecommendations(KeywordsDto keywords) {
        HttpHeaders headers = new HttpHeaders();
        String URL = pythonServiceEndpoint + "/api/recommend/keyword";

        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<KeywordsDto> request = new HttpEntity<>(keywords, headers);

        try {
            ResponseEntity<List<RecommendationDto>> response = restTemplate.exchange(
                    URL, HttpMethod.POST, request, new ParameterizedTypeReference<>() {}
            );

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                saveRecommendationsToFile(response.getBody());
                return ResponseEntity.ok("JSON received and saved successfully in: " + dataPath);
            } else {
                return ResponseEntity.status(response.getStatusCode()).body("Error while fetching recommendations");
            }
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to send keywords to python");
        }
    }

    public ResponseEntity<String> getRecommendationsWithPassedCourses(PassedAndPossibleCoursesDto UUIDs) {
        HttpHeaders headers = new HttpHeaders();
        String URL = pythonServiceEndpoint + "/api/recommend/byPassed";

        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<PassedAndPossibleCoursesDto> request = new HttpEntity<>(UUIDs, headers);

        try {
            ResponseEntity<List<RecommendationDto>> response = restTemplate.exchange(
                    URL, HttpMethod.POST, request, new ParameterizedTypeReference<>() {}
            );

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                saveRecommendationsToFile(response.getBody());
                return ResponseEntity.ok("JSON received and saved successfully in: " + dataPath);
            } else {
                return ResponseEntity.status(response.getStatusCode()).body("Error while fetching recommendations");
            }
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to send keywords to python");
        }
    }

    /**
     * Use: Testing connection with Python ONLY
     * @param data
     * @return
     */
    public String sendTest(List<CourseDto> data) {
        HttpHeaders headers = new HttpHeaders();

        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<List<CourseDto>> request = new HttpEntity<>(data, headers);

        ResponseEntity<String> response = restTemplate.exchange(pythonServiceEndpoint + "api/test",
                HttpMethod.POST, request, String.class);

        return response.getBody();
    }

    private <T> void saveRecommendationsToFile(List<T> body) {
        ObjectMapper objectMapper = new ObjectMapper();
        File file = new File(String.format(dataPath, "received_payload.json"));

        try {
            File parentDir = file.getParentFile();
            if (parentDir != null && !parentDir.exists()) {
                parentDir.mkdirs();
            }

            objectMapper.writeValue(file, body);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
