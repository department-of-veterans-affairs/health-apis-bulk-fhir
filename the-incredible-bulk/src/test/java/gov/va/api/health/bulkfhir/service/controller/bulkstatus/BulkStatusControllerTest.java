package gov.va.api.health.bulkfhir.service.controller.patient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import gov.va.api.health.bulkfhir.api.bulkstatus.PublicationFileStatusResponse;
import gov.va.api.health.bulkfhir.service.controller.bulkstatus.BulkStatusController;
import gov.va.api.health.bulkfhir.service.status.StatusEntity;
import gov.va.api.health.bulkfhir.service.status.StatusRepository;
import gov.va.api.health.ids.api.IdentityService;
import gov.va.api.health.ids.api.ResourceIdentity;
import gov.va.api.health.ids.client.IdEncoder;
import java.time.Instant;
import java.util.List;
import javax.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

@ExtendWith(MockitoExtension.class)
public class BulkStatusControllerTest {

  @Mock StatusRepository repo;

  @Mock HttpServletRequest request;

  @Mock IdentityService identityService;

  BulkStatusController controller() {
    return BulkStatusController.builder()
        .repository(repo)
        .bulkBaseUrl("http://fake-va.gov")
        .bulkFileUrlPath("/bulk/publication")
        .identityService(identityService)
        .build();
  }

  @Test
  void getBulkStatusReturns400WhenInvalidAcceptHeaderIsProvided() {
    ResponseEntity<PublicationFileStatusResponse> response =
        controller().getBulkStatus("whatevah", "BOOM");
    assertThat(response.getStatusCodeValue()).isEqualTo(400);
    assertThat(response.getBody()).isNull();
  }

  @Test
  void getBulkStatusReturns404WhenNoPublicationStatusEntitiesAreFound() {
    when(identityService.lookup(any()))
        .thenReturn(
            List.of(
                ResourceIdentity.builder()
                    .identifier("EXPOSED")
                    .resource("test.gov")
                    .system("BULK")
                    .build()));
    when(repo.findByPublicationId("EXPOSED")).thenReturn(List.of());
    ResponseEntity<PublicationFileStatusResponse> response =
        controller().getBulkStatus("application/json", "CRACKME");
    assertThat(response.getStatusCodeValue()).isEqualTo(404);
    assertThat(response.getBody()).isNull();
  }

  @Test
  void getBulkStatusReturns404WhenPublicationLookupReturnsNoResults() {
    when(identityService.lookup(any())).thenReturn(List.of());
    ResponseEntity<PublicationFileStatusResponse> response =
        controller().getBulkStatus("application/fhir+json", "empty");
    assertThat(response.getStatusCodeValue()).isEqualTo(404);
    assertThat(response.getBody()).isNull();
  }

  @Test
  void getBulkStatusReturns404WhenPublicationLookupReturnsNull() {
    when(identityService.lookup(any())).thenReturn(null);
    ResponseEntity<PublicationFileStatusResponse> response =
        controller().getBulkStatus("application/fhir+json", "cantcrackme");
    assertThat(response.getStatusCodeValue()).isEqualTo(404);
    assertThat(response.getBody()).isNull();
  }

  @Test
  void getBulkStatusReturns404WhenPublicationStringCouldNotBeDecoded() {
    when(identityService.lookup(any())).thenThrow(new IdEncoder.BadId("WAT IS THIS"));
    ResponseEntity<PublicationFileStatusResponse> response =
        controller().getBulkStatus("application/fhir+json", "NODECODE");
    assertThat(response.getStatusCodeValue()).isEqualTo(404);
    assertThat(response.getBody()).isNull();
  }

  @Test
  void getBulkStatusReturnsAppropriateSuccessfulResponse() {
    long now = Instant.now().toEpochMilli();
    when(identityService.lookup(any()))
        .thenReturn(
            List.of(
                ResourceIdentity.builder()
                    .identifier("EXPOSED")
                    .resource("/test")
                    .system("BULK")
                    .build()));
    when(repo.findByPublicationId("EXPOSED"))
        .thenReturn(
            List.of(
                StatusEntity.builder()
                    .publicationEpoch(now)
                    .fileName("FILE1")
                    .publicationId("EXPOSED")
                    .recordsPerFile(10)
                    .build()));
    ResponseEntity<PublicationFileStatusResponse> response =
        controller().getBulkStatus("application/fhir+json", "CRACKME");
    PublicationFileStatusResponse expected =
        PublicationFileStatusResponse.builder()
            .requiresAccessToken(true)
            .request("http://fake-va.gov/test")
            .creationDate(Instant.ofEpochMilli(now))
            .output(
                List.of(
                    PublicationFileStatusResponse.FileLocation.builder()
                        .type("Patient")
                        .url("http://fake-va.gov/bulk/publication/EXPOSED/FILE1.ndjson")
                        .build()))
            .extension(
                PublicationFileStatusResponse.Extension.builder()
                    .creationDate(Instant.ofEpochMilli(now))
                    .id("EXPOSED")
                    .recordsPerFile(10)
                    .build())
            .build();
    assertThat(response.getStatusCodeValue()).isEqualTo(200);
    assertThat(response.getBody()).isEqualTo(expected);
  }
}
