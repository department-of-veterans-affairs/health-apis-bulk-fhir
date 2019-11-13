package gov.va.api.health.bulkfhir.service.controller.publication;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import gov.va.api.health.argonaut.api.resources.Patient;
import gov.va.api.health.bulkfhir.api.internal.FileBuildResponse;
import gov.va.api.health.bulkfhir.service.dataquery.client.DataQueryBatchClient;
import gov.va.api.health.bulkfhir.service.dataquery.client.DataQueryBatchClient.RequestFailed;
import gov.va.api.health.bulkfhir.service.filebuilder.FileBuildRequest;
import gov.va.api.health.bulkfhir.service.filebuilder.FileClaim;
import gov.va.api.health.bulkfhir.service.filebuilder.FileClaimant;
import java.util.List;
import java.util.concurrent.ExecutionException;
import lombok.SneakyThrows;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class NonDistributedFileWorkerTest {
  @Mock DataQueryBatchClient dq;
  @Mock FileClaimant claimant;

  private FileClaim claim() {
    return FileClaim.builder()
        .request(FileBuildRequest.builder().publicationId("p").fileId("f").build())
        .page(3)
        .count(1234)
        .build();
  }

  @Test
  void failedResponseWhenPatientFetchFails() {
    when(dq.requestPatients(3, 1234)).thenThrow(new RequestFailed("xxx"));
    var response = worker().buildFile(claim());
    assertThrows(ExecutionException.class, response::get);
  }

  private List<Patient> refactorMeToBeReusableSamplePatients() {
    return List.of(Patient.builder().build());
  }

  @Test
  @SneakyThrows
  void successResponseWhenFileIsSaved() {
    when(dq.requestPatients(3, 1234)).thenReturn(refactorMeToBeReusableSamplePatients());
    var result = worker().buildFile(claim());
    assertThat(result.get())
        .isEqualTo(FileBuildResponse.builder().publicationId("p").fileId("f").build());
  }

  NonDistributedFileWorker worker() {
    return NonDistributedFileWorker.builder().claimant(claimant).dataQuery(dq).build();
  }
}
