package gov.va.api.health.bulkfhir.tests;

import gov.va.api.health.autoconfig.configuration.JacksonConfig;
import gov.va.api.health.sentinel.BasicTestClient;
import gov.va.api.health.sentinel.TestClient;
import lombok.experimental.UtilityClass;

/**
 * Test clients for interacting with different services (bulk-fhir, data-query) in a {@link
 * SystemDefinition}.
 */
@UtilityClass
public class TestClients {

  static TestClient bulkFhir() {
    return BasicTestClient.builder()
        .service(SystemDefinitions.systemDefinition().bulkFhir())
        .contentType("application/json")
        .mapper(JacksonConfig::createMapper)
        .build();
  }
}
