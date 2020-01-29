package gov.va.api.health.bulkfhir.service.controller.bulkstatus;

import gov.va.api.health.autoconfig.logging.Loggable;
import gov.va.api.health.bulkfhir.api.bulkstatus.PublicationFileStatusResponse;
import gov.va.api.health.bulkfhir.service.status.StatusEntity;
import gov.va.api.health.bulkfhir.service.status.StatusRepository;
import gov.va.api.health.ids.api.IdentityService;
import gov.va.api.health.ids.api.IdentityService.LookupFailed;
import gov.va.api.health.ids.api.ResourceIdentity;
import gov.va.api.health.ids.client.IdEncoder.BadId;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.Builder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@Validated
@RestController
@Loggable
@RequestMapping(
    value = {"dstu2/bulk"},
    produces = {"application/json"})
public class BulkStatusController {

  /** The list of valid values for the Accept header for an $export request. */
  private static List<String> VALID_ACCEPT_HEADER_VALUES =
      List.of("application/fhir+json", "application/json");

  private final StatusRepository repository;

  private final String bulkBaseUrl;

  private final String bulkFileBaseUrl;

  private final IdentityService identityService;

  @Builder
  BulkStatusController(
      @Value("${incrediblebulk.public-url}") String bulkBaseUrl,
      @Value("${incrediblebulk.public-bulk-file-path}") String bulkFileUrlPath,
      @Autowired StatusRepository repository,
      @Autowired IdentityService identityService) {
    this.bulkBaseUrl = bulkBaseUrl;
    this.bulkFileBaseUrl = bulkBaseUrl + bulkFileUrlPath;
    this.repository = repository;
    this.identityService = identityService;
  }

  /**
   * Convert the list of publication file status objects to output FileLocations.
   *
   * @param fileStatuses The file statuses to convert
   * @return A list of FileLocation with the appropriate fully qualified URL to the file.
   */
  private List<PublicationFileStatusResponse.FileLocation> createFileOutputList(
      List<StatusEntity> fileStatuses) {
    return fileStatuses.stream()
        .map(
            (file) ->
                PublicationFileStatusResponse.FileLocation.builder()
                    .type("Patient")
                    .url(
                        bulkFileBaseUrl
                            + "/"
                            + file.publicationId()
                            + "/"
                            + file.fileName()
                            + ".ndjson")
                    .build())
        .collect(Collectors.toList());
  }

  /**
   * Decode the publication request string into a ResourceIdentity object.
   *
   * @param publicationRequestString The string to decode
   * @return The decoded ResourceIdentity or null if one could not be found.
   */
  private ResourceIdentity decodePublicationRequest(String publicationRequestString) {
    try {
      List<ResourceIdentity> foundIdentities = identityService.lookup(publicationRequestString);
      if (foundIdentities == null || foundIdentities.isEmpty()) {
        return null;
      }
      return foundIdentities.get(0);
    } catch (BadId | LookupFailed e) {
      log.error("Failed to decode publication request string {}", publicationRequestString, e);
      return null;
    }
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
      @RequestHeader("Accept") String acceptHeader,
      @PathVariable("id") String publicationRequestString) {
    if (!VALID_ACCEPT_HEADER_VALUES.contains(acceptHeader)) {
      log.info("Invalid Accept header received for bulk status request");
      return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
    }

    ResourceIdentity publicationRequest = decodePublicationRequest(publicationRequestString);
    if (publicationRequest == null) {
      return new ResponseEntity<>(HttpStatus.NOT_FOUND);
    }
    List<StatusEntity> fileStatuses =
        repository.findByPublicationId(publicationRequest.identifier());
    if (fileStatuses == null || fileStatuses.isEmpty()) {
      /*
       * No statuses were found for the given publication id
       */
      log.info("No publication data found for publication {}", publicationRequest.identifier());
      return new ResponseEntity<>(HttpStatus.NOT_FOUND);
    }
    List<PublicationFileStatusResponse.FileLocation> outputFiles =
        createFileOutputList(fileStatuses);
    /*
     * Pull out the first status entity to use for the boiler plate information of the response
     */
    StatusEntity firstStatusEntity = fileStatuses.get(0);
    return ResponseEntity.ok(
        PublicationFileStatusResponse.builder()
            .requiresAccessToken(true)
            .request(bulkBaseUrl + publicationRequest.resource())
            .transactionTime(Instant.ofEpochMilli(firstStatusEntity.publicationEpoch()))
            .output(outputFiles)
            /* Error is required, but will remain empty */
            .error(List.of())
            .extension(
                Optional.of(
                    PublicationFileStatusResponse.Extension.builder()
                        .creationDate(Instant.ofEpochMilli(firstStatusEntity.publicationEpoch()))
                        .id(firstStatusEntity.publicationId())
                        .recordsPerFile(firstStatusEntity.recordsPerFile())
                        .build()))
            .build());
  }
}
