package gov.va.api.health.bulkfhir.service.controller.status;

import static gov.va.api.health.bulkfhir.service.controller.status.PublicationExceptions.assertDoesNotExist;
import static gov.va.api.health.bulkfhir.service.controller.status.PublicationExceptions.assertPublicationFound;
import static gov.va.api.health.bulkfhir.service.controller.status.PublicationExceptions.assertRecordsPerFile;

import gov.va.api.health.bulkfhir.api.internal.ClearHungRequest;
import gov.va.api.health.bulkfhir.api.internal.PublicationRequest;
import gov.va.api.health.bulkfhir.api.internal.PublicationStatus;
import java.time.Duration;
import java.time.Instant;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import javax.validation.Valid;
import lombok.Builder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@Validated
@RestController
@RequestMapping(
  value = {"internal/publication"},
  produces = {"application/json"}
)
class InternalPublicationController {

  private final StatusRepository repository;

  private final PublicationStatusTransformer transformer;

  private final DataQueryBatchClient dataQuery;

  @Builder
  InternalPublicationController(
      @Autowired StatusRepository repository,
      @Autowired DataQueryBatchClient dataQuery,
      @Autowired(required = false) PublicationStatusTransformer transformer) {
    this.repository = repository;
    this.dataQuery = dataQuery;
    this.transformer =
        transformer == null ? new DefaultPublicationStatusTransformer() : transformer;
  }

  @Scheduled(cron = "${hung-status.cron.expression}")
  public void automaticallyClearHungPublication() {
    /* We'll grab this property from the system properties, but just in case we'll
     * default to 15 minutes (900000 ms).
     */
    String statusAllowedHangTime = System.getProperty("hung-status.allowed.hangtime", "15 minutes");
    doClearHungStatusMarkers(
        ClearHungRequest.builder().hangTime(statusAllowedHangTime).build().hangTime());
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public void createPublication(@Valid @RequestBody PublicationRequest request) {
    int existing = repository.countByPublicationId(request.publicationId());
    assertDoesNotExist(existing != 0, request.publicationId());
    var resources = dataQuery.requestPatientCount();
    assertRecordsPerFile(request.recordsPerFile(), resources.maxRecordsPerPage());
    var publicationEpoch = Instant.now().toEpochMilli();
    var fileName = "Patient-%04d";
    int page = 1;
    int remaining = resources.count();
    var entities = new LinkedList<StatusEntity>();
    while (remaining > 0) {
      int thisFileSize = Math.min(request.recordsPerFile(), remaining);
      entities.add(
          StatusEntity.builder()
              .publicationId(request.publicationId())
              .publicationEpoch(publicationEpoch)
              .recordsPerFile(request.recordsPerFile())
              .fileName(String.format(fileName, page))
              .page(page)
              .count(thisFileSize)
              .build());
      page++;
      remaining -= thisFileSize;
    }
    repository.saveAll(entities);
  }

  @DeleteMapping(path = "{id}")
  public void deletePublication(@PathVariable("id") String publicationId) {
    var deleted = repository.deleteByPublicationId(publicationId);
    assertPublicationFound(deleted > 0, publicationId);
  }

  public void doClearHungStatusMarkers(Duration allowedHangTime) {
    var entities = repository.findByStatusInProgress();
    var nowEpoch = Instant.now().toEpochMilli();
    entities
        .stream()
        .filter(Objects::nonNull)
        .filter(e -> (nowEpoch - e.buildStartEpoch()) > allowedHangTime.toMillis())
        .forEach(
            s -> {
              s.buildStartEpoch(0);
              repository.save(s);
            });
  }

  @GetMapping
  public List<String> getPublicationIds() {
    return repository.findDistinctPublicationIds();
  }

  @GetMapping(path = "{id}")
  public PublicationStatus getPublicationStatus(@PathVariable("id") String publicationId) {
    var entities = repository.findByPublicationId(publicationId);
    assertPublicationFound(!entities.isEmpty(), publicationId);
    return transformer.apply(entities);
  }

  @PostMapping(path = "hung")
  @ResponseStatus(HttpStatus.OK)
  public void manuallyClearHungPublication(@RequestBody ClearHungRequest clearHungRequest) {
    log.info("Manual kick off of /hung endpoint: {}", clearHungRequest.hangTime().toString());
    doClearHungStatusMarkers(clearHungRequest.hangTime());
  }
}
