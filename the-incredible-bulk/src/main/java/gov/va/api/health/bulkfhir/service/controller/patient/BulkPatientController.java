package gov.va.api.health.bulkfhir.service.controller.patient;

import gov.va.api.health.bulkfhir.api.internal.BuildStatus;
import gov.va.api.health.bulkfhir.api.internal.PublicationStatus;
import gov.va.api.health.bulkfhir.service.controller.publication.DefaultPublicationStatusTransformer;
import gov.va.api.health.bulkfhir.service.controller.publication.PublicationStatusTransformer;
import gov.va.api.health.bulkfhir.service.status.StatusEntity;
import gov.va.api.health.bulkfhir.service.status.StatusRepository;
import gov.va.api.health.dstu2.api.elements.Narrative;
import gov.va.api.health.dstu2.api.resources.OperationOutcome;
import gov.va.api.health.ids.api.IdentityService;
import gov.va.api.health.ids.api.Registration;
import gov.va.api.health.ids.api.ResourceIdentity;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import javax.servlet.http.HttpServletRequest;
import lombok.Builder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@Validated
@RestController
@RequestMapping(
  value = {"Patient"},
  produces = {"application/json"}
)
class BulkPatientController {

  /** The list of valid output formats for an $export request. */
  private static List<String> VALID_OUTPUT_FORMATS =
      List.of("application/fhir+ndjson", "application/ndjson", "ndjson");

  /** The list of valid values for the Accept header for an $export request. */
  private static List<String> VALID_ACCEPT_HEADER_VALUES =
      List.of("application/fhir+json", "application/json");

  private final StatusRepository repository;

  private final PublicationStatusTransformer transformer;

  private final String bulkStatusUrl;

  private final IdentityService identityService;

  @Builder
  BulkPatientController(
      @Value("${incrediblebulk.url}") String baseUrl,
      @Value("${incrediblebulk.bulk-status-path:/services/fhir/v0/dstu2/bulk}")
          String bulkStatusPath,
      @Autowired StatusRepository repository,
      @Autowired IdentityService identityService,
      @Autowired(required = false) PublicationStatusTransformer transformer) {
    this.bulkStatusUrl = baseUrl + bulkStatusPath;
    this.repository = repository;
    this.identityService = identityService;
    this.transformer =
        transformer == null ? new DefaultPublicationStatusTransformer() : transformer;
  }

  /**
   * Build the OperationOutcome when no publications are ready to be distributed.
   *
   * @return An OperationOutcome indicating this call should be tried again later.
   */
  private OperationOutcome buildNotReadyOperationOutcome() {
    return OperationOutcome.builder()
        .id(UUID.randomUUID().toString())
        .resourceType("OperationOutcome")
        .text(
            Narrative.builder()
                .status(Narrative.NarrativeStatus.additional)
                .div("<div>No publications available, try again later.</div>")
                .build())
        .issue(
            Collections.singletonList(
                OperationOutcome.Issue.builder()
                    .severity(OperationOutcome.Issue.IssueSeverity.information)
                    .code("incomplete")
                    .build()))
        .build();
  }

  /**
   * Encode the original request along with the publication id.
   *
   * @param request The http request
   * @param completedPublication The completed publication
   * @return The encoded string for the request and publication id
   */
  private String encodeRequestAndPublicationName(
      HttpServletRequest request, PublicationStatus completedPublication) {
    String requestUrl = request.getRequestURI() + "?" + request.getQueryString();
    List<Registration> encoded =
        identityService.register(
            List.of(
                ResourceIdentity.builder()
                    .identifier(completedPublication.publicationId())
                    .resource(requestUrl)
                    .system("BULK")
                    .build()));
    if (encoded.isEmpty()) {
      /*
       * Somehow no results came back from encoding, just return the publication id
       */
      return completedPublication.publicationId();
    }
    return encoded.get(0).uuid();
  }

  /**
   * An informational endpoint that will provide the status URL for the most recent completed
   * publication.
   *
   * @param headers The headers for the request
   * @param outputFormat The outputFormat query parameter for the request
   * @return A response entity that can contain an OperationOutcome
   */
  @GetMapping("$export")
  public ResponseEntity<OperationOutcome> export(
      HttpServletRequest request,
      @RequestHeader Map<String, String> headers,
      @RequestParam(name = "_outputFormat") String outputFormat) {
    if (isRequestInvalid(headers, outputFormat)) {
      log.info("Invalid bulk export request received");
      return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
    }
    PublicationStatus completedPublication = getMostRecentCompletedPublication();
    if (completedPublication == null) {
      log.info("No completed publications found.");
      return new ResponseEntity<>(buildNotReadyOperationOutcome(), HttpStatus.SERVICE_UNAVAILABLE);
    } else {
      HttpHeaders responseHeaders = new HttpHeaders();
      responseHeaders.add(
          "Content-Location",
          bulkStatusUrl + "/" + encodeRequestAndPublicationName(request, completedPublication));
      return new ResponseEntity<>(responseHeaders, HttpStatus.ACCEPTED);
    }
  }

  /**
   * Get the most recently fully completed publication (if one exists) and return the status.
   *
   * @return The most recently fully completed publication, or null if no publications have been
   *     completed
   */
  private PublicationStatus getMostRecentCompletedPublication() {
    List<String> publicationIdsByCreation = repository.findDistinctPublicationIds();
    for (String publication : publicationIdsByCreation) {
      List<StatusEntity> entities = repository.findByPublicationId(publication);
      PublicationStatus status = transformer.apply(entities);
      if (status.overallStatus() == BuildStatus.COMPLETE) {
        return status;
      }
    }
    return null;
  }

  /**
   * Determine if the export request is invalid or not.
   *
   * @param headers The headers from the request
   * @param outputFormat The _outputFormat parameter from the request
   * @return true if the request is invalid, false otherwise
   */
  private boolean isRequestInvalid(Map<String, String> headers, String outputFormat) {
    if (!VALID_OUTPUT_FORMATS.contains(outputFormat)) {
      return true;
    }
    try {
      if (!VALID_ACCEPT_HEADER_VALUES.contains(headers.get("accept"))) {
        return true;
      }
      return !"respond-async".equals(headers.get("prefer"));
    } catch (NullPointerException e) {
      log.info("One of the accept or prefer headers were not provided.", e);
      return true;
    }
  }
}
