package gov.va.api.health.bulkfhir.service.controller.publication;

import static gov.va.api.health.bulkfhir.service.config.AsyncConfig.PUBLICATION_BUILD_EXECUTOR;

import com.fasterxml.jackson.databind.ObjectMapper;
import gov.va.api.health.argonaut.api.resources.Patient;
import gov.va.api.health.bulkfhir.anonymizer.ClassPathResourceBasedNames;
import gov.va.api.health.bulkfhir.anonymizer.ResourceBasedSyntheticData;
import gov.va.api.health.bulkfhir.anonymizer.SaltedType5UuidGenerator;
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
import lombok.Builder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@Builder
public class NonDistributedFileWorker implements FileBuildWorker {

  private final DataQueryBatchClient dataQuery;

  private final FileClaimant claimant;

  private final BulkFileWriter fileWriter;

  private final ObjectMapper jacksonMapper;

  private final int familyNameOffset;

  private final String saltKey;

  private final String uuidSeed;

  /**
   * Default constructor.
   *
   * @param dataQuery The data query client
   * @param claimant The file claimant
   * @param fileWriter The file writer
   * @param jacksonMapper The jackson object mapper
   * @param familyNameOffset The family name offset value
   * @param saltKey The salt key to use for id anonymization
   */
  public NonDistributedFileWorker(
      @Autowired DataQueryBatchClient dataQuery,
      @Autowired FileClaimant claimant,
      @Autowired BulkFileWriter fileWriter,
      @Autowired ObjectMapper jacksonMapper,
      @Value("${anonymization.family-name-offset}") int familyNameOffset,
      @Value("${anonymization.salt}") String saltKey,
      @Value("${anonymization.uuid-seed}") String uuidSeed) {
    this.dataQuery = dataQuery;
    this.claimant = claimant;
    this.fileWriter = fileWriter;
    this.jacksonMapper = jacksonMapper;
    this.familyNameOffset = familyNameOffset;
    this.saltKey = saltKey;
    this.uuidSeed = uuidSeed;
  }

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
                .familyNameOffset(familyNameOffset)
                .build())
        .idGenerator(
            SaltedType5UuidGenerator.builder()
                .resource("Patient")
                .saltKey(saltKey)
                .seed(uuidSeed)
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
