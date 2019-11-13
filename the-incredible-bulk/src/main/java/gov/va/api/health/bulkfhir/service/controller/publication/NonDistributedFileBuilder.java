package gov.va.api.health.bulkfhir.service.controller.publication;

import gov.va.api.health.bulkfhir.api.internal.FileBuildResponse;
import gov.va.api.health.bulkfhir.service.filebuilder.FileBuildRequest;
import gov.va.api.health.bulkfhir.service.filebuilder.FileBuildWorker;
import gov.va.api.health.bulkfhir.service.filebuilder.FileBuilder;
import gov.va.api.health.bulkfhir.service.filebuilder.FileBuilderExceptions.ClaimFailed;
import gov.va.api.health.bulkfhir.service.filebuilder.FileClaim;
import gov.va.api.health.bulkfhir.service.filebuilder.FileClaimant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@Builder
@AllArgsConstructor(onConstructor = @__({@Autowired}))
public class NonDistributedFileBuilder implements FileBuilder {

  private final FileClaimant claimant;
  private final FileBuildWorker worker;

  @Override
  public FileBuildResponse buildFile(FileBuildRequest request) {
    var claim = claim(request);
    worker.buildFile(claim);
    return submittedResponse(claim);
  }

  private FileClaim claim(FileBuildRequest request) {
    try {
      return claimant.tryClaim(request);
    } catch (ClaimFailed e) {
      throw e;
    } catch (Exception e) {
      log.error("Failed to claim {}", request, e);
      throw new ClaimFailed(request.publicationId(), request.fileId(), e);
    }
  }

  private FileBuildResponse submittedResponse(FileClaim claim) {
    return FileBuildResponse.builder()
        .publicationId(claim.request().publicationId())
        .fileId(claim.request().fileId())
        .build();
  }
}
