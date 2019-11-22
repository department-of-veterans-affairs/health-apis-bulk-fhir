package gov.va.api.health.bulkfhir.service.controller.bulkstatus;

import gov.va.api.health.bulkfhir.api.bulkstatus.PublicationFileStatusResponse;
import gov.va.api.health.bulkfhir.service.status.StatusEntity;
import gov.va.api.health.bulkfhir.service.status.StatusRepository;
import gov.va.api.health.ids.api.IdentityService;
import gov.va.api.health.ids.api.ResourceIdentity;
import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;
import lombok.Builder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@Validated
@RestController
@RequestMapping(
  value = {"bulk"},
  produces = {"application/json"}
)
public class BulkStatusController {
  /** The list of valid values for the Accept header for an $export request. */
  private static List<String> VALID_ACCEPT_HEADER_VALUES =
      List.of("application/fhir+json", "application/json");

  private final StatusRepository repository;

  private final String bulkFileBaseUrl;

  private final IdentityService identityService;

  @Builder
  BulkStatusController(
      @Value("${incrediblebulk.public-url}") String bulkBaseUrl,
      // TODO need to add to application.properties
      @Value("${incrediblebulk.public-bulk-file-path:/services/fhir/v0/dstu2/bulk/publication}")
          String bulkFileUrlPath,
      @Autowired StatusRepository repository,
      @Autowired IdentityService identityService) {
    this.bulkFileBaseUrl = bulkBaseUrl + bulkFileUrlPath;
    this.repository = repository;
    this.identityService = identityService;
  }

  /**
   * Build and return the bulk status response.
   *
   * @param publicationRequestString The encoded publication request containing the original
   *     kick-off request and the publication id to retrieve
   * @return The publication file status with a link to all of the relevant files
   */
  @GetMapping(path = "{id}")
  public ResponseEntity<PublicationFileStatusResponse> getBulkStatus(
      @PathVariable("id") String publicationRequestString) {
    // TODO move this to decodePublicationRequest() method and check we get results, etc.
    ResourceIdentity publicationRequest = identityService.lookup(publicationRequestString).get(0);
    // TODO move this to createFileOutputList() method and make sure we find publications
    List<StatusEntity> fileStatuses =
        repository.findByPublicationId(publicationRequest.identifier());
    List<PublicationFileStatusResponse.FileLocation> outputFiles =
        fileStatuses
            .stream()
            .map(
                (a) ->
                    PublicationFileStatusResponse.FileLocation.builder()
                        .type("PATIENT")
                        .url(bulkFileBaseUrl + "/" + a.publicationId() + "/" + a.fileName())
                        .build())
            .collect(Collectors.toList());
    return ResponseEntity.ok(
        PublicationFileStatusResponse.builder()
            .requiresAccessToken(true)
            .request(publicationRequest.resource())
            .creationDate(Instant.ofEpochMilli(fileStatuses.get(0).publicationEpoch()))
            .output(outputFiles)
            .extension(
                PublicationFileStatusResponse.Extension.builder()
                    .creationDate(Instant.ofEpochMilli(fileStatuses.get(0).publicationEpoch()))
                    .id(fileStatuses.get(0).publicationId())
                    .recordsPerFile(fileStatuses.get(0).recordsPerFile())
                    .build())
            .build());
  }
}
