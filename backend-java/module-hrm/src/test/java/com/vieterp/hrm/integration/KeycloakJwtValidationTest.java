package com.vieterp.hrm.integration;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.*;
import java.util.Base64;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class KeycloakJwtValidationTest {

    @Value("${server.port}")
    private int port;

    private final TestRestTemplate restTemplate = new TestRestTemplate();

    @Test
    void getEmployee_withoutJwt_returns401() {
        String url = "http://localhost:" + port + "/api/v1/hrm/employees";
        ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
    }

    @Test
    void getEmployee_withInvalidJwt_returns401() {
        String url = "http://localhost:" + port + "/api/v1/hrm/employees";
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer invalid.token.here");
        ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, new HttpEntity<>(headers), String.class);
        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
    }

    @Test
    void actuatorHealth_isPublic() {
        String url = "http://localhost:" + port + "/actuator/health";
        ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
        assertEquals(HttpStatus.OK, response.getStatusCode());
    }
}