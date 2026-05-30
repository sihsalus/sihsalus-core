package org.sihsalus.core.boot;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.web.client.RestTemplate;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = {
      "sihsalus.ocl.static-import.enabled=false",
      "sihsalus.admin.password=test-admin-password"
    })
@Testcontainers
public class ComposeSmokeIntegrationTest {

  @Container
  static PostgreSQLContainer<?> postgres =
      new PostgreSQLContainer<>("postgres:16-alpine")
          .withDatabaseName("sihsalus")
          .withUsername("sihsalus")
          .withPassword("sihsalus");

  @DynamicPropertySource
  static void overrideProperties(DynamicPropertyRegistry registry) {
    registry.add("spring.datasource.url", postgres::getJdbcUrl);
    registry.add("spring.datasource.username", postgres::getUsername);
    registry.add("spring.datasource.password", postgres::getPassword);
  }

  @LocalServerPort int port;

  RestTemplate restTemplate = new RestTemplate();

  @Test
  @Timeout(180)
  public void readinessEndpointIsUp() throws Exception {
    String url = "http://localhost:" + port + "/actuator/health/readiness";
    // retry loop: TestRestTemplate will block until app is started; allow some retries
    long deadline = System.currentTimeMillis() + 120_000;
    ResponseEntity<String> resp = null;
    while (System.currentTimeMillis() < deadline) {
      try {
        resp = restTemplate.getForEntity(url, String.class);
        if (resp != null && resp.getStatusCode().is2xxSuccessful()) break;
      } catch (Exception e) {
        Thread.sleep(1000);
      }
    }
    assertThat(resp).as("readiness response").isNotNull();
    assertThat(resp.getStatusCode().is2xxSuccessful()).isTrue();
    assertThat(resp.getBody()).containsIgnoringCase("UP");
  }
}
