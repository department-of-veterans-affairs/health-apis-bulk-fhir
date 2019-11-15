package gov.va.api.health.bulkfhir.service.controller.publication;

import static gov.va.api.health.bulkfhir.service.config.AsyncConfig.PUBLICATION_BUILD_EXECUTOR;

import com.fasterxml.jackson.databind.ObjectMapper;
import gov.va.api.health.argonaut.api.resources.Patient;
import gov.va.api.health.bulkfhir.anonymizer.ClassPathResourceBasedNames;
import gov.va.api.health.bulkfhir.anonymizer.ResourceBasedSyntheticData;
import gov.va.api.health.bulkfhir.anonymizer.patient.PatientAnonymizer;
import gov.va.api.health.bulkfhir.api.internal.FileBuildResponse;
import gov.va.api.health.bulkfhir.service.controller.JsonStringConverter;
import gov.va.api.health.bulkfhir.service.dataquery.client.DataQueryBatchClient;
import gov.va.api.health.bulkfhir.service.dataquery.client.DataQueryBatchClient.DataQueryBatchClientException;
import gov.va.api.health.bulkfhir.service.filebuilder.BulkFileWriter;
import gov.va.api.health.bulkfhir.service.filebuilder.FileBuildWorker;
import gov.va.api.health.bulkfhir.service.filebuilder.FileBuilderExceptions.BuildFailed;
import gov.va.api.health.bulkfhir.service.filebuilder.FileClaim;
import gov.va.api.health.bulkfhir.service.filebuilder.FileClaimant;
import gov.va.api.health.dstu2.api.resources.Resource;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import java.util.stream.Stream;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@Builder
@AllArgsConstructor(onConstructor = @__({@Autowired}))
public class NonDistributedFileWorker implements FileBuildWorker {

  private final DataQueryBatchClient dataQuery;

  private final FileClaimant claimant;

  private final BulkFileWriter fileWriter;

  private final ObjectMapper jacksonMapper;

  @Override
  @Async(PUBLICATION_BUILD_EXECUTOR)
  public CompletableFuture<FileBuildResponse> buildFile(FileClaim claim) {
    try {
      List<Patient> patients = fetchPatients(claim);
      writePatients(
          claim,
          patients
              .stream()
              .map(patientAnonymizer())
              .map(jsonStringConverter())
              .filter(Objects::nonNull));
      releaseClaim(claim);
      return successfulResponse(claim);
    } catch (Exception e) {
      releaseClaim(claim);
      return failedResponse(claim, e);
    }
  }

  private CompletableFuture<FileBuildResponse> failedResponse(FileClaim claim, Exception cause) {
    return CompletableFuture.failedFuture(
        new BuildFailed(claim.request().publicationId(), claim.request().fileId(), cause));
  }

  private List<Patient> fetchPatients(FileClaim claim) {
    try {
      log.info("Fetching patients: {}", claim);
      List<Patient> patients = dataQuery.requestPatients(claim.page(), claim.count());
      log.info("Found {} patients to bulk process.", patients.size());
      return patients;
    } catch (DataQueryBatchClientException e) {
      log.error("Failed to fetch patients", e);
      throw e;
    }
  }

  private Function<? super Resource, String> jsonStringConverter() {
    return JsonStringConverter.builder().jacksonMapper(jacksonMapper).build();
  }

  private Function<Patient, Patient> patientAnonymizer() {
    return PatientAnonymizer.builder()
        .syntheticData(
            ResourceBasedSyntheticData.builder()
                .names(ClassPathResourceBasedNames.instance())
                .build())
        .build();
  }

  private void releaseClaim(FileClaim claim) {
    try {
      claimant.completeClaim(claim.request());
    } catch (Exception e) {
      /*
       * We don't want to explode if we fail to complete the claim for some reason.
       * Worst case this fails and we may rebuild the file again later
       * when the hung completion is cleared.
       */
      log.error("We failed to mark this claim {} as complete.", claim, e);
    }
  }

  private CompletableFuture<FileBuildResponse> successfulResponse(FileClaim claim) {
    return CompletableFuture.completedFuture(
        FileBuildResponse.builder()
            .publicationId(claim.request().publicationId())
            .fileId(claim.request().fileId())
            .build());
  }

  private void writePatients(FileClaim claim, Stream<String> patients) throws Exception {
    try {
      fileWriter.writeFile(claim, patients);
    } catch (Exception e) {
      log.error("Failed to write patient file", e);
      throw e;
    }
  }
}
