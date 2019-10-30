package gov.va.api.health.bulkfhir.service.controller.status;

import gov.va.api.health.bulkfhir.api.internal.PublicationRequest;
import gov.va.api.health.bulkfhir.api.internal.PublicationStatus;
import java.util.LinkedList;
import java.util.List;
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
    produces = {"application/json"})
class InternalPublicationController {

  private final StatusRepository repository;

  private final PublicationStatusTransformer transformer;

  @Builder
  InternalPublicationController(
      @Autowired StatusRepository repository,
      @Autowired(required = false) PublicationStatusTransformer transformer) {
    this.repository = repository;
    this.transformer =
        transformer == null ? new DefaultPublicationStatusTransformer() : transformer;
  }

  // TODO DELETE ME
  @GetMapping
  List<StatusEntity> _deleteMe() {
    var result = new LinkedList<StatusEntity>();
    repository.findAll().forEach(result::add);
    return result;
  }

  // TODO DELETE ME
  @GetMapping(value = {"/add"})
  void _deleteMeAddOne() {
    repository.save(
        StatusEntity.builder()
            .publicationId("pubby")
            .publicationEpoch(System.currentTimeMillis())
            .recordsPerFile(10)
            .firstRecord(1)
            .lastRecord(10)
            .fileName("filegumbo")
            .buildStartEpoch(0)
            .buildCompleteEpoch(0)
            .build());
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public void createPublication(@RequestBody PublicationRequest request) {}

  @DeleteMapping(path = "{id}")
  public void deletePublication(@PathVariable("id") String publicationId) {}

  @GetMapping
  public List<String> getPublicationIds() {
    return null;
  }

  @GetMapping(path = "{id}")
  public PublicationStatus getPublicationStatus(@PathVariable("id") String publicationId) {
    return null;
  }
}
