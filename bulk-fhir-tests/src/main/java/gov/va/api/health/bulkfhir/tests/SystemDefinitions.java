package gov.va.api.health.bulkfhir.tests;

import gov.va.api.health.sentinel.Environment;
import gov.va.api.health.sentinel.SentinelProperties;
import gov.va.api.health.sentinel.ServiceDefinition;
import java.util.Optional;
import lombok.experimental.UtilityClass;

/**
 * {@link SystemDefinition}s for different environments. {@link #systemDefinition()} method provides
 * the appropriate implementation for the current environment.
 */
@UtilityClass
public class SystemDefinitions {

  /** Service definitions for lab testing. */
  private static SystemDefinition lab() {
    String url = "https://dev-api.va.gov";
    return SystemDefinition.builder()
        .bulkFhir(serviceDefinition("incredible-bulk", url, 443, "/services/fhir/v0/dstu2"))
        .build();
  }

  /** Service definitions for local testing. */
  private static SystemDefinition local() {
    String url = "http://localhost";
    return SystemDefinition.builder()
        .bulkFhir(serviceDefinition("incredible-bulk", url, 8091, ""))
        .build();
  }

  /** Service definitions for production testing. */
  private static SystemDefinition production() {
    String url = "https://api.va.gov";
    return SystemDefinition.builder()
        .bulkFhir(serviceDefinition("incredible-bulk", url, 443, "/services/fhir/v0/dstu2"))
        .build();
  }

  /** Service definitions for qa testing. */
  private static SystemDefinition qa() {
    String url = "https://blue.qa.lighthouse.va.gov";
    return SystemDefinition.builder()
        .bulkFhir(serviceDefinition("incredible-bulk", url, 443, "/fhir/v0/dstu2"))
        .build();
  }

  private static ServiceDefinition serviceDefinition(
      String name, String url, int port, String apiPath) {
    return ServiceDefinition.builder()
        .url(SentinelProperties.optionUrl(name, url))
        .port(port)
        .apiPath(SentinelProperties.optionApiPath(name, apiPath))
        .accessToken(() -> Optional.ofNullable(null))
        .build();
  }

  /** Service definitions for staging testing. */
  private static SystemDefinition staging() {
    String url = "https://blue.staging.lighthouse.va.gov";
    return SystemDefinition.builder()
        .bulkFhir(serviceDefinition("incredible-bulk", url, 443, "/fhir/v0/dstu2"))
        .build();
  }

  /** Service definitions for staging testing. */
  private static SystemDefinition stagingLab() {
    String url = "https://blue.staging-lab.lighthouse.va.gov";
    return SystemDefinition.builder()
        .bulkFhir(serviceDefinition("incredible-bulk", url, 443, "/fhir/v0/dstu2"))
        .build();
  }

  /** Return the applicable system definition for the current environment. */
  public static SystemDefinition systemDefinition() {
    switch (Environment.get()) {
      case LAB:
        return lab();
      case LOCAL:
        return local();
      case PROD:
        return production();
      case QA:
        return qa();
      case STAGING:
        return staging();
      case STAGING_LAB:
        return stagingLab();
      default:
        throw new IllegalArgumentException("Unknown sentinel environment: " + Environment.get());
    }
  }
}
