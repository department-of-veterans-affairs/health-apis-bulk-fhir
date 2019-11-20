package gov.va.api.health.bulkfhir.service.controller.patient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import gov.va.api.health.bulkfhir.service.controller.publication.DefaultPublicationStatusTransformer;
import gov.va.api.health.bulkfhir.service.status.StatusEntity;
import gov.va.api.health.bulkfhir.service.status.StatusRepository;
import gov.va.api.health.dstu2.api.resources.OperationOutcome;
import gov.va.api.health.ids.api.IdentityService;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

@ExtendWith(MockitoExtension.class)
public class BulkPatientControllerTest {

  @Mock StatusRepository repo;

  @Mock HttpServletRequest request;

  @Mock IdentityService identityService;

  BulkPatientController controller() {
    return BulkPatientController.builder()
        .repository(repo)
        .baseUrl("http://fake-va.gov")
        .bulkStatusPath("/STATUSNOW")
        .identityService(identityService)
        .transformer(new DefaultPublicationStatusTransformer())
        .build();
  }

  @Test
  void
      exportReturnsContentLocationHeaderForFirstPublicationWhenMultipleCompletedPublicationExist() {
    when(repo.findDistinctPublicationIds()).thenReturn(List.of("1", "2"));
    long now = Instant.now().toEpochMilli();
    when(repo.findByPublicationId("1"))
        .thenReturn(
            List.of(
                StatusEntity.builder()
                    .publicationId("1")
                    .publicationEpoch(now)
                    .buildCompleteEpoch(now)
                    .buildStartEpoch(now)
                    .recordsPerFile(10)
                    .build()));
    when(request.getRequestURL())
        .thenReturn(new StringBuffer("http://fake-va.gov/Patient/$export"));
    when(request.getQueryString()).thenReturn("_outputFormat=ndjson");
    Map<String, String> headers = new HashMap<>();
    headers.put("accept", "application/fhir+json");
    headers.put("prefer", "respond-async");
    ResponseEntity<OperationOutcome> response = controller().export(request, headers, "ndjson");
    verify(repo, never()).findByPublicationId("2");
    assertThat(response.getStatusCodeValue()).isEqualTo(202);
    assertThat(response.getBody()).isNull();
    List<String> contentLocationHeaders = response.getHeaders().get("Content-Location");
    assertThat(contentLocationHeaders).isNotNull();
    assertThat(contentLocationHeaders.size()).isEqualTo(1);
    assertThat(contentLocationHeaders.get(0)).isEqualTo("http://fake-va.gov/STATUSNOW/1");
    // TODO validate the actual value of Content-Location
  }

  @Test
  void exportReturnsContentLocationHeaderWhenACompletedPublicationExists() {
    when(repo.findDistinctPublicationIds()).thenReturn(List.of("1", "2"));
    long now = Instant.now().toEpochMilli();
    when(repo.findByPublicationId("2"))
        .thenReturn(
            List.of(
                StatusEntity.builder()
                    .publicationId("2")
                    .publicationEpoch(now)
                    .buildCompleteEpoch(now)
                    .buildStartEpoch(now)
                    .recordsPerFile(10)
                    .build()));
    when(repo.findByPublicationId("1"))
        .thenReturn(
            List.of(
                StatusEntity.builder()
                    .publicationId("1")
                    .publicationEpoch(now)
                    .buildStartEpoch(now)
                    .recordsPerFile(10)
                    .build()));
    when(request.getRequestURL())
        .thenReturn(new StringBuffer("http://fake-va.gov/Patient/$export"));
    when(request.getQueryString()).thenReturn("_outputFormat=ndjson");
    Map<String, String> headers = new HashMap<>();
    headers.put("accept", "application/fhir+json");
    headers.put("prefer", "respond-async");
    ResponseEntity<OperationOutcome> response = controller().export(request, headers, "ndjson");
    assertThat(response.getStatusCodeValue()).isEqualTo(202);
    assertThat(response.getBody()).isNull();
    List<String> contentLocationHeaders = response.getHeaders().get("Content-Location");
    assertThat(contentLocationHeaders).isNotNull();
    assertThat(contentLocationHeaders.size()).isEqualTo(1);
    assertThat(contentLocationHeaders.get(0)).isEqualTo("http://fake-va.gov/STATUSNOW/2");
    // TODO validate the actual value of Content-Location
  }

  @Test
  void exportReturnsOperationOutcomeWhenNoCompletedPublicationsExist() {
    when(repo.findDistinctPublicationIds()).thenReturn(List.of("1", "2", "3"));
    long now = Instant.now().toEpochMilli();
    /*
     * None of the publications should be completed so we can just reuse the same response for them all in this test.
     */
    when(repo.findByPublicationId(any()))
        .thenReturn(
            List.of(
                StatusEntity.builder()
                    .publicationId("1")
                    .publicationEpoch(now)
                    .buildStartEpoch(now)
                    .recordsPerFile(10)
                    .build()));
    Map<String, String> headers = new HashMap<>();
    headers.put("accept", "application/fhir+json");
    headers.put("prefer", "respond-async");
    ResponseEntity<OperationOutcome> response = controller().export(request, headers, "ndjson");
    assertThat(response.getStatusCodeValue()).isEqualTo(503);
    assertThat(response.getBody()).isNotNull();
    assertThat(response.getBody().text()).isNotNull();
    assertThat(response.getBody().text().div()).contains("try again later");
  }

  @Test
  void exportReturnsOperationOutcomeWhenNoPublicationsExist() {
    when(repo.findDistinctPublicationIds()).thenReturn(new ArrayList<>());
    Map<String, String> headers = new HashMap<>();
    headers.put("accept", "application/fhir+json");
    headers.put("prefer", "respond-async");
    ResponseEntity<OperationOutcome> response = controller().export(request, headers, "ndjson");
    assertThat(response.getStatusCodeValue()).isEqualTo(503);
    assertThat(response.getBody()).isNotNull();
    assertThat(response.getBody().text()).isNotNull();
    assertThat(response.getBody().text().div()).contains("try again later");
  }

  @Test
  void exportWithInvalidAcceptHeaderReturnsBadRequest() {
    Map<String, String> headers = new HashMap<>();
    headers.put("accept", "y u no like me");
    headers.put("prefer", "respond-async");
    ResponseEntity<OperationOutcome> response =
        controller().export(request, headers, "application/ndjson");
    assertThat(response.getStatusCodeValue()).isEqualTo(400);
  }

  @Test
  void exportWithInvalidOutputFormatReturnsBadRequest() {
    Map<String, String> headers = new HashMap<>();
    headers.put("accept", "application/fhir+json");
    headers.put("prefer", "respond-async");
    ResponseEntity<OperationOutcome> response = controller().export(request, headers, "IMFAKE");
    assertThat(response.getStatusCodeValue()).isEqualTo(400);
  }

  @Test
  void exportWithInvalidPreferHeaderReturnsBadRequest() {
    Map<String, String> headers = new HashMap<>();
    headers.put("accept", "application/json");
    headers.put("prefer", "callmemaybe");
    ResponseEntity<OperationOutcome> response =
        controller().export(request, headers, "application/fhir+ndjson");
    assertThat(response.getStatusCodeValue()).isEqualTo(400);
  }

  @Test
  void exportWithMissingAcceptHeaderReturnsBadRequest() {
    Map<String, String> headers = new HashMap<>();
    headers.put("prefer", "respond-async");
    ResponseEntity<OperationOutcome> response =
        controller().export(request, headers, "application/ndjson");
    assertThat(response.getStatusCodeValue()).isEqualTo(400);
  }

  @Test
  void exportWithMissingPreferHeaderReturnsBadRequest() {
    Map<String, String> headers = new HashMap<>();
    headers.put("accept", "application/json");
    ResponseEntity<OperationOutcome> response = controller().export(request, headers, "ndjson");
    assertThat(response.getStatusCodeValue()).isEqualTo(400);
  }
}
