package gov.va.api.health.bulkfhir.service.controller.publication;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import gov.va.api.health.argonaut.api.resources.Patient;
import gov.va.api.health.bulkfhir.api.internal.FileBuildResponse;
import gov.va.api.health.bulkfhir.service.dataquery.client.DataQueryBatchClient;
import gov.va.api.health.bulkfhir.service.dataquery.client.DataQueryBatchClient.RequestFailed;
import gov.va.api.health.bulkfhir.service.filebuilder.BulkFileWriter;
import gov.va.api.health.bulkfhir.service.filebuilder.FileBuildRequest;
import gov.va.api.health.bulkfhir.service.filebuilder.FileClaim;
import gov.va.api.health.bulkfhir.service.filebuilder.FileClaimant;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.stream.Stream;
import lombok.SneakyThrows;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class NonDistributedFileWorkerTest {

  @Mock DataQueryBatchClient dq;

  @Mock FileClaimant claimant;

  @Mock BulkFileWriter fileWriter;

  @Mock ObjectMapper objectMapper;

  private FileClaim claim() {
    return FileClaim.builder()
        .request(FileBuildRequest.builder().publicationId("p").fileId("f").build())
        .page(3)
        .count(1234)
        .build();
  }

  @Test
  void failedResponseIsReturnedWhenFileWriterGivesException() throws Exception {
    when(dq.requestPatients(3, 1234)).thenReturn(refactorMeToBeReusableSamplePatients());
    doThrow(new IllegalArgumentException("NOPE")).when(fileWriter).writeFile(any(), any());
    var response = worker().buildFile(claim());
    assertThrows(ExecutionException.class, response::get);
  }

  @Test
  void failedResponseWhenPatientFetchFails() {
    when(dq.requestPatients(3, 1234)).thenThrow(new RequestFailed("xxx"));
    var response = worker().buildFile(claim());
    assertThrows(ExecutionException.class, response::get);
  }

  @Test
  @SneakyThrows
  void fileWriterIsGivenTheProperDataToWrite() {
    when(dq.requestPatients(3, 1234)).thenReturn(refactorMeToBeReusableSamplePatients());
    when(objectMapper.writeValueAsString(any())).thenReturn("HI");
    FileClaim claim = claim();
    worker().buildFile(claim);
    ArgumentCaptor<FileClaim> fileClaim = ArgumentCaptor.forClass(FileClaim.class);
    ArgumentCaptor<Stream<String>> resourceStream = ArgumentCaptor.forClass(Stream.class);
    verify(fileWriter).writeFile(fileClaim.capture(), resourceStream.capture());
    verify(claimant).completeClaim(claim.request());
    assertThat(fileClaim.getValue()).isEqualTo(claim());
    assertThat(resourceStream.getValue().count()).isEqualTo(1);
  }

  private List<Patient> refactorMeToBeReusableSamplePatients() {
    return List.of(Patient.builder().id("12345V67890").build());
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
    return NonDistributedFileWorker.builder()
        .claimant(claimant)
        .dataQuery(dq)
        .fileWriter(fileWriter)
        .jacksonMapper(objectMapper)
        .familyNameOffset(1000)
        .build();
  }
}
