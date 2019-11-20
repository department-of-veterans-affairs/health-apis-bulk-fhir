package gov.va.api.health.bulkfhir.service.controller.publication;

import static gov.va.api.health.bulkfhir.service.controller.publication.PublicationExceptions.assertDoesNotExist;
import static gov.va.api.health.bulkfhir.service.controller.publication.PublicationExceptions.assertPublicationFound;
import static gov.va.api.health.bulkfhir.service.controller.publication.PublicationExceptions.assertRecordsPerFile;

import gov.va.api.health.bulkfhir.api.internal.ClearHungRequest;
import gov.va.api.health.bulkfhir.api.internal.FileBuildResponse;
import gov.va.api.health.bulkfhir.api.internal.PublicationRequest;
import gov.va.api.health.bulkfhir.api.internal.PublicationStatus;
import gov.va.api.health.bulkfhir.service.dataquery.client.DataQueryBatchClient;
import gov.va.api.health.bulkfhir.service.filebuilder.FileBuildRequest;
import gov.va.api.health.bulkfhir.service.filebuilder.FileBuilder;
import gov.va.api.health.bulkfhir.service.filebuilder.FileToBuildManager;
import gov.va.api.health.bulkfhir.service.status.StatusEntity;
import gov.va.api.health.bulkfhir.service.status.StatusRepository;
import java.time.Duration;
import java.time.Instant;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;
import lombok.Builder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
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

  private final FileBuilder fileBuilder;

  private final FileToBuildManager fileToBuildManager;

  private final StatusRepository repository;

  private final PublicationStatusTransformer transformer;

  private final DataQueryBatchClient dataQuery;

  @Builder
  InternalPublicationController(
      @Autowired FileBuilder fileBuilder,
      @Autowired StatusRepository repository,
      @Autowired DataQueryBatchClient dataQuery,
      @Autowired FileToBuildManager fileToBuildManager,
      @Autowired(required = false) PublicationStatusTransformer transformer) {
    this.fileBuilder = fileBuilder;
    this.repository = repository;
    this.dataQuery = dataQuery;
    this.fileToBuildManager = fileToBuildManager;
    this.transformer =
        transformer == null ? new DefaultPublicationStatusTransformer() : transformer;
  }

  @PostMapping(path = "{id}/file/{fileId}")
  @ResponseStatus(HttpStatus.ACCEPTED)
  public FileBuildResponse buildFile(
      @PathVariable("id") String publicationId, @PathVariable("fileId") String fileId) {
    return fileBuilder.buildFile(
        FileBuildRequest.builder().publicationId(publicationId).fileId(fileId).build());
  }

  @PostMapping("any/file/next")
  public FileBuildResponse buildNextFile(HttpServletResponse response) {
    FileBuildRequest fileToBuild = fileToBuildManager.getNextFileToBuild();
    if (fileToBuild == null) {
      /*
       * There were no files to build.
       */
      response.setStatus(HttpStatus.NO_CONTENT.value());
      return null;
    }

    response.setStatus(HttpStatus.ACCEPTED.value());
    return buildFile(fileToBuild.publicationId(), fileToBuild.fileId());
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

  private void doClearHungStatusMarkers(Duration allowedHangTime) {
    // Note: allowedHangTime.toString() will print out in a Duration format (i.e. PT15M)
    log.info(
        "Cleaning up publications with IN_PROGRESS status markers older than: {}",
        allowedHangTime.toString());
    var entities = repository.findByStatusInProgress();
    var nowEpoch = Instant.now().toEpochMilli();
    var resetEntities =
        entities
            .stream()
            .filter(Objects::nonNull)
            .filter(e -> (nowEpoch - e.buildStartEpoch()) > allowedHangTime.toMillis())
            .collect(Collectors.toList());
    resetEntities.stream().forEach(s -> s.buildStartEpoch(0));
    repository.saveAll(resetEntities);
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
  public void manuallyClearHungPublications(@RequestBody ClearHungRequest clearHungRequest) {
    doClearHungStatusMarkers(clearHungRequest.hangTime());
  }
}
