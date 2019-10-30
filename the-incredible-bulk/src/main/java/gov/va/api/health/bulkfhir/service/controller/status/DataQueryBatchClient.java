package gov.va.api.health.bulkfhir.service.controller.status;

import gov.va.api.health.argonaut.api.resources.Patient;
import java.util.List;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Value;

public interface DataQueryBatchClient {

  ResourceCount requestPatientCount();

  List<Patient> requestPatients(int page, int count);

  @Value
  @Builder
  @AllArgsConstructor(access = AccessLevel.PRIVATE)
  public static class ResourceCount {
    String resourceType;
    int count;
    int maxRecordsPerPage;
  }
}
