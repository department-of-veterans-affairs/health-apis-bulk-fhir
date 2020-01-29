package gov.va.api.health.bulkfhir.service;

import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@SpringBootTest
@TestPropertySource(
    properties = {
      "spring.datasource.driver-class-name=org.h2.Driver",
      "spring.datasource.password=unset",
      "spring.datasource.url=jdbc:h2:mem:bulk",
      "spring.datasource.username=unset",
      "spring.h2.console.enabled=true",
      "spring.jpa.properties.hibernate.globally_quoted_identifiers=false",
    })
public class ApplicationTest {

  @Test
  public void contextLoads() {
    /* Verifies that the application starts. */
    assertTrue(true);
  }
}
