package gov.va.api.health.bulkfhir.service.controller.publication;

import static gov.va.api.health.bulkfhir.service.config.AsyncConfig.PUBLICATION_BUILD_EXECUTOR;

import gov.va.api.health.argonaut.api.resources.Patient;
import gov.va.api.health.bulkfhir.api.internal.FileBuildResponse;
import gov.va.api.health.bulkfhir.service.dataquery.client.DataQueryBatchClient;
import gov.va.api.health.bulkfhir.service.dataquery.client.DataQueryBatchClient.DataQueryBatchClientException;
import gov.va.api.health.bulkfhir.service.filebuilder.FileBuildWorker;
import gov.va.api.health.bulkfhir.service.filebuilder.FileBuilderExceptions.BuildFailed;
import gov.va.api.health.bulkfhir.service.filebuilder.FileClaim;
import gov.va.api.health.bulkfhir.service.filebuilder.FileClaimant;
import java.util.List;
import java.util.concurrent.CompletableFuture;
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

  private void anonymizePatients() {
    /* Configure the patient anonymizer and execute it here. */
  }

  @Override
  @Async(PUBLICATION_BUILD_EXECUTOR)
  public CompletableFuture<FileBuildResponse> buildFile(FileClaim claim) {
    try {
      fetchPatients(claim);
      anonymizePatients();
      writePatients();
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
      log.info("OMG SO MANY PATIENTS. THIS {} MANY. THAT'S A BIG BOI MANY.", patients.size());
      return patients;
    } catch (DataQueryBatchClientException e) {
      log.error("Failed to fetch patients", e);
      throw e;
    }
  }

  private void releaseClaim(FileClaim claim) {
    /*
     * Exceptions generated here _must_ be handled in the best way possible. Do not let them leak
     * out.
     */
    // e.g. claimant.completeClaim(claim.request());
  }

  private CompletableFuture<FileBuildResponse> successfulResponse(FileClaim claim) {
    return CompletableFuture.completedFuture(
        FileBuildResponse.builder()
            .publicationId(claim.request().publicationId())
            .fileId(claim.request().fileId())
            .build());
  }

  private void writePatients() {
    /* Create a BulkFileWrite interface that is injected into this class. Let it do the work. */
  }
}
