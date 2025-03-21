package edu.aua.course_recommendation.service;

import edu.aua.course_recommendation.dto.CourseDto;
import edu.aua.course_recommendation.dto.KeywordsDto;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PythonService {

    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${python.service.url}")
    private String pythonServiceEndpoint;

    public String sendCourses(List<CourseDto> data) {
        HttpHeaders headers = new HttpHeaders();

        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<List<CourseDto>> request = new HttpEntity<>(data, headers);

        ResponseEntity<String> response = restTemplate.exchange(pythonServiceEndpoint + "api/vectorize",
                HttpMethod.PUT, request, String.class);

        return response.getBody();
    }

    /** For freshman: keywords
     *
      * @param keywords List of keywords that interest the student
     * @return
     */
    public String sendKeywordsRecommendations(KeywordsDto keywords) {
        HttpHeaders headers = new HttpHeaders();
        System.out.println(pythonServiceEndpoint);

        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<KeywordsDto> request = new HttpEntity<>(keywords, headers);
        ResponseEntity<String> response = restTemplate.exchange(pythonServiceEndpoint + "api/recommend/keyword", HttpMethod.POST, request, String.class);
        return response.getBody();
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
}
